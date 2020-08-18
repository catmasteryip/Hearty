from scipy.io import wavfile

import numpy as np
import pandas as pd

import mel
from sklearn.preprocessing import MinMaxScaler

from keras.models import Sequential, Model, load_model
from sklearn.metrics import mean_squared_error


def load_sound(filename):
    '''
        Return sampling_rate and period for .wav file at filename
        Args:
            filename(str): directory of the .wav file
        Returns:
            sampling_rate(int): sampling rate
            wave.T(list(int)): 
    '''
    sampling_rate, wave = wavfile.read(filename)
    assert (len(wave.T) > 4)
    return sampling_rate, wave.T


def divide_single_wave_into_smaller_chunk(output_duration=3, wave=None, sampling_rate=None, thresh=True, shift=0):
    '''
        Divide single wave into chunks
    '''
    shift_abs = int(sampling_rate * shift)
    chunk_length = sampling_rate*output_duration
    min_length = (output_duration)*sampling_rate - 2
    wave_chunks = []
    temp_wave = wave.copy()
    count = 0
    temp_wave = temp_wave[shift_abs:]
    while(len(temp_wave) >= min_length):
        count += 1
        new_chunk = temp_wave[0:chunk_length]
        wave_chunks.append(new_chunk)
        temp_wave = temp_wave[chunk_length*count:]
    return wave_chunks


def minmax(wave):
    scaler = MinMaxScaler()
    scaler.fit(wave.reshape(-1, 1))
    wave = scaler.transform(wave.reshape(-1, 1))
    wave = wave.reshape(8192*3,)
    return wave


def audio2spec(filename):
    sr, wav = load_sound(filename)
#     wav_chunks = []
#     assert(sr == 8192)
    chunks = divide_single_wave_into_smaller_chunk(4, wav, sr)
#     wav_chunks.append(chunks)
    c = chunks[0]
#     for i, c in enumerate(chunks):
    spec = mel.pretty_spectrogram(
        c.astype('float64'), fft_size=512, step_size=128, log=True)
    return spec


def transform(spectrogram):
    model = load_model('models/32-16-16-32.hdf5')

    sp_reshaped = np.expand_dims(spectrogram, -1)
    sp_reshaped = np.expand_dims(sp_reshaped, axis=0)

    pred = model.predict(sp_reshaped)

    pred_reshaped = np.squeeze(pred)

    return pred_reshaped


def back_to_audio(pred_spectrogram):
    recovered_audio_orig = mel.invert_pretty_spectrogram(
        pred_spectrogram, fft_size=512, step_size=128, log=True, n_iter=40)
    return recovered_audio_orig


def denoise(wav_path):
    spec = audio2spec(wav_path)
    denoised_spec = transform(spec)
    denoised = back_to_audio(denoised_spec)
    return denoised
