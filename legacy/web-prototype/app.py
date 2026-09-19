from flask import Flask, render_template, request
import whisper
from transformers import pipeline
from gtts import gTTS
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
app = Flask(__name__, template_folder=str(BASE_DIR))

# -------------------------------
# LOAD MODELS ONCE
# -------------------------------

print("Loading Whisper model...")
whisper_model = whisper.load_model("base")

print("Loading Summarization model...")
summarizer = pipeline(
    "summarization",
    model="facebook/bart-large-cnn"
)

# -------------------------------
# ROUTE
# -------------------------------

@app.route("/", methods=["GET", "POST"])
def index():

    transcript = ""
    summary = ""
    reply_audio = ""

    if request.method == "POST":

        # --------------------------------
        # AUDIO UPLOAD PROCESS
        # --------------------------------
        if "audio" in request.files and request.files["audio"].filename != "":

            audio_file = request.files["audio"]
            file_path = BASE_DIR / "temp_audio.mp3"
            audio_file.save(file_path)

            # Speech to text
            result = whisper_model.transcribe(file_path)
            transcript = result["text"]

            # Summarization
            summary_result = summarizer(
                transcript,
                max_length=80,
                min_length=25,
                do_sample=False
            )

            summary = summary_result[0]["summary_text"]

            os.remove(file_path)

        # --------------------------------
        # TEXT → SPEECH
        # --------------------------------
        if "user_text" in request.form:

            user_text = request.form["user_text"]

            if user_text.strip() != "":
                tts = gTTS(user_text)

                static_dir = BASE_DIR / "static"
                static_dir.mkdir(exist_ok=True)
                audio_path = static_dir / "reply.mp3"
                tts.save(str(audio_path))
                reply_audio = "static/reply.mp3"

    return render_template(
        "index.html",
        transcript=transcript,
        summary=summary,
        reply_audio=reply_audio
    )


if __name__ == "__main__":
    app.run(debug=True)
