package com.aihearingassist;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Contents;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.MessageCallback;
import com.google.ai.edge.litertlm.SamplerConfig;
import com.google.ai.edge.litertlm.ThinkingConfig;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Java bridge around LiteRT-LM.
 *
 * The LiteRT-LM Android AAR is currently compiled with newer Kotlin metadata
 * than the app's stable Kotlin/Compose toolchain. Keeping direct LiteRT-LM API
 * calls in Java avoids Kotlin metadata compatibility failures while still
 * allowing the app to use the local model at runtime.
 */
final class LiteRtLmSummaryBridge implements AutoCloseable {
    private final String modelPath;
    private final String cacheDir;
    private final int maxContextTokens;
    private final int maxOutputTokens;
    private Engine engine;
    private final Object conversationLock = new Object();
    private final AtomicLong cancellationVersion = new AtomicLong();
    private Conversation activeConversation;

    LiteRtLmSummaryBridge(
            String modelPath,
            String cacheDir,
            int maxContextTokens,
            int maxOutputTokens
    ) {
        this.modelPath = modelPath;
        this.cacheDir = cacheDir;
        this.maxContextTokens = maxContextTokens;
        this.maxOutputTokens = maxOutputTokens;
    }

    void prepare() {
        getOrCreateEngine();
    }

    String summarize(String systemInstruction, String prompt, Consumer<String> onPartial) throws Exception {
        long version = cancellationVersion.get();
        Engine activeEngine = getOrCreateEngine();
        if (version != cancellationVersion.get()) throw new CancellationException();
        // This static-template model receives the task inside the user prompt.
        // Do not duplicate it as a system preface (extra prefill and memory).
        Contents instruction = null;
        SamplerConfig samplerConfig = new SamplerConfig(1, 1.0, 0.0, 7);
        ConversationConfig conversationConfig = new ConversationConfig(
                instruction,
                Collections.emptyList(),
                Collections.emptyList(),
                samplerConfig,
                false,
                null, // Retain model channel metadata so reasoning never leaks into the gist.
                Collections.singletonMap("enable_thinking", false),
                null,
                false,
                maxOutputTokens,
                new ThinkingConfig(false, 0),
                false
        );

        try (Conversation conversation = activeEngine.createConversation(conversationConfig)) {
            synchronized (conversationLock) {
                if (version != cancellationVersion.get()) throw new CancellationException();
                activeConversation = conversation;
            }
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            StringBuffer text = new StringBuffer();
            try {
                conversation.sendMessageAsync(prompt, new MessageCallback() {
                    @Override public void onMessage(Message message) {
                        if (version != cancellationVersion.get()) return;
                        String chunk = extractText(message);
                        if (!chunk.isEmpty()) {
                            text.append(chunk);
                            onPartial.accept(text.toString());
                        }
                    }
                    @Override public void onDone() { completed.countDown(); }
                    @Override public void onError(Throwable error) {
                        failure.set(error);
                        completed.countDown();
                    }
                });
                if (!completed.await(45, TimeUnit.SECONDS)) {
                    cancel();
                    // Cancellation is a request, not proof that native callbacks have stopped.
                    // Wait on this background worker before destroying their conversation/engine.
                    completed.await();
                    throw new TimeoutException("Local generation exceeded 45 seconds");
                }
                if (version != cancellationVersion.get()) throw new CancellationException();
                Throwable error = failure.get();
                if (error != null) throw new IllegalStateException(error.getMessage(), error);
                return text.toString();
            } finally {
                synchronized (conversationLock) { activeConversation = null; }
            }
        }
    }

    void cancel() {
        cancellationVersion.incrementAndGet();
        synchronized (conversationLock) {
            if (activeConversation != null) activeConversation.cancelProcess();
        }
    }

    private Engine getOrCreateEngine() {
        if (engine != null) {
            return engine;
        }
        new File(cacheDir).mkdirs();
        EngineConfig config = new EngineConfig(
                modelPath,
                new Backend.CPU(Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() - 2)), null),
                null,
                null,
                maxContextTokens,
                null,
                cacheDir
        );
        Engine created = new Engine(config);
        try {
            created.initialize();
        } catch (RuntimeException | LinkageError error) {
            try { created.close(); }
            catch (RuntimeException cleanupError) { error.addSuppressed(cleanupError); }
            throw error;
        }
        engine = created;
        return created;
    }

    private static String extractText(Message message) {
        StringBuilder builder = new StringBuilder();
        for (Content content : message.getContents().getContents()) {
            if (content instanceof Content.Text) {
                builder.append(((Content.Text) content).getText());
            }
        }
        return builder.toString();
    }

    @Override
    public void close() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
    }
}
