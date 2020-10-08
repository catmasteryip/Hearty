from pydub import AudioSegment
from helper import denoise
# cannot use pydub because ffmpeg is not compatible with chaquopy
from scipy.io.wavfile import write


def denoising(sound_path):
    # read .wav original track
    sound = AudioSegment.from_file(sound_path)
    sampling_rate = sound.frame_rate

    # instantiate cache sound file paths
    denoised_path = sound_path.replace('.wav', '_denoised.wav')
    mp3_path = denoised_path.replace('.wav', '.mp3')

    # apply denoise algo on original track in .wav format
    denoised = denoise(sound_path)

    # save denoised .wav track
    write(denoised_path, rate=sampling_rate, data=denoised)

    return denoised_path
