from pydub import AudioSegment
from helper import denoise
# from scipy.io import wavfile
from scipy.io.wavfile import write


def denoising(sound_path):
    # read .wav original track
    sound = AudioSegment.from_file(sound_path)
    # amplify .wav original track
    # sound = sound.apply_gain(-sound.max_dBFS)
    # get sampling_rate for exports
    sampling_rate = sound.frame_rate

    # instantiate cache sound file paths
    # wav_path = sound_path.replace('.3gp', '.wav')
    denoised_path = sound_path.replace('.wav', '_denoised.wav')
    mp3_path = denoised_path.replace('.wav', '.mp3')

    # export original track in .wav format
    # sound.export(wav_path, format="wav")

    # apply denoise algo on original track in .wav format
    denoised = denoise(sound_path)
    # denoised = denoised.astype('float32')

    # save denoised .wav track
    write(denoised_path, rate=sampling_rate, data=denoised)

    # read denoised .wav track and save as .mp3
    # denoised_wav = AudioSegment.from_file(denoised_path, format='wav')
    # denoised_wav = denoised_wav.apply_gain(-denoised_wav.max_dBFS)
    # denoised_wav.export(mp3_path, format="mp3")
    return denoised_path
