import matplotlib.pyplot as plt
from scipy.io.wavfile import read
from io import BytesIO
import base64

def showWave(path):
    sound = read(path)
    sound = sound[1]

    plt.plot(sound)
    plt.axis('off')
    # Make Matplotlib write to BytesIO file object and grab
    # return the object's string

    figfile = BytesIO()
    plt.savefig(figfile, format='png')
    figdata_png = figfile.getvalue()
    plt.close()
    return figdata_png