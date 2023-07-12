# enviroCar Android App

This is the app for the enviroCar platform. (www.envirocar.org)

## Description

### XFCD Mobile Data Collection and Analysis

**Collecting and analyzing vehicle sensor data**

enviroCar Mobile is an Android application for smartphones that can be used to collect Extended Floating Car Data (XFCD). The app communicates with an OBD2 Bluetooth adapter while the user drives. This enables read access to data from the vehicle’s engine control. The data is recorded along with the smartphone’s GPS position data.The driver can view statistics about his drives and publish his data as open data. The latter happens by uploading tracks to the enviroCar server, where the data is available under the ODbL license for further analysis and use. The data can also be viewed and analyzed via the enviroCar website. enviroCar Mobile is one of the enviroCar Citizen Science Platform’s components (www.envirocar.org).


**Key Technologies**

-	Android
-	Java

**Benefits**

-	Easy collection of Extended Floating Car Data
- Optional automation of data collection and upload
- Estimation of fuel consumption and CO2 emissions
- Publishing anonymized track data as Open Data
- Map based visualization of track data and track statistics


## Quick Start 


### Installation

Use the [Google Play Store](https://play.google.com/store/apps/details?id=org.envirocar.app) to install the app on your device.

We are planning to include the project into F-Droid in the near future.

## Development

This software uses the gradle build system and is optimized to work within Android Studio 1.3+.
The setup of the source code should be straightforward. Just follow the Android Studio guidelines
for existing projects.

### Setting up the mapbox SDK
The enviroCar App project uses the ``Mapbox Maven repository``. **Mapbox is a mapping and location cloud platform for developers.**
To build the project you need the mapbox account, you can create an account for free from [here](https://account.mapbox.com/auth/signup/). 
Once you have created an account, you will need to configure credentials 

### Configure credentials
1. From your account's [tokens page](https://account.mapbox.com/access-tokens/), click the **Create a token** button.
2. Give your token a name and do check that you have checked ``Downloads:Read`` scope.
3. Make sure you copy your token and save it somehwere as you will not be able to see the token again. 

### Configure your secret token
1. This is a secret token, and we will use it in ``gradle.properties`` file. You should not expose the token in public, that's why add ``gradle.properties`` in ``.gitignore`` . It's also possible to store the sercret token in your local user's _gradle.properties_ file, usually stored at _«USER_HOME»/.gradle/gradle.properties_. 
2. Now open the ``gradle.properties`` file and add this line ``MAPBOX_DOWNLOADS_TOKEN = <your-secret-token> ``. The secret token has to be pasted without any quote marks. 
``MAPBOX_DOWNLOADS_TOKEN=sk.dutaksgjdvlsayVDSADUTLASDs@!ahsvdaslud*JVAS@%DLUTSVgdJLA&&>Hdval.sujdvadvasuydgalisy``(this is just a random string, not a real token)
3. That't it. You are good to go!

If you are still facing any problem, checkout the [Mapbox guide](https://docs.mapbox.com/android/maps/guides/install/) or feel free to [create an issue](https://github.com/enviroCar/enviroCar-app/issues/new)

## License

The enviroCar App is licensed under the [GNU General Public License, Version 3](https://github.com/enviroCar/enviroCar-app/blob/master/LICENSE).

## Recorded Parameters
|Parametername	        |Unit   	|   	|   	|   	|
|---	                |---	|---	|---	|---	|
|Speed 	                |km/h  	|   	|   	|   	|
|Mass-Air-Flow (MAF)   	|l/s   	|   	|   	|   	|
|Calculated (MAF)       |g/s   	|   	|   	|   	|
|RPM                    |u/min 	|   	|   	|   	|
|Intake Temperature     |c   	|   	|   	|   	|
|Intake Pressure        |kPa  	|   	|   	|   	|
|CO2                    |kg/h  	|   	|   	|   	|
|CO2 (GPS-based)        |kg/h  	|   	|   	|   	|
|Consumption            |l/h   	|   	|   	|   	|
|Consumption (GPS-based)|l/h   	|   	|   	|   	|
|Throttle Position      |%   	|   	|   	|   	|
|Engine Load            |%   	|   	|   	|   	|
|GPS Accuracy           |%   	|   	|   	|   	|
|GPS Speed              |km/h  	|   	|   	|   	|
|GPS Bearing            |deg   	|   	|   	|   	|
|GPS Altitude           |m  	|   	|   	|   	|
|GPS PDOP               |precision   	|   	|   	|   	|
|GPS HDOP               |precision   	|   	|   	|   	|
|GPS VDOP               |precision   	|   	|   	|   	|
|Lambda Voltage         |V   	|   	|   	|   	|
|Lambda Voltage ER      |ratio 	|       |   	|   	|
|Lambda Current         |A   	|   	|   	|   	|
|Lambda Current ER      |ratio  |    	|   	|   	|
|Fuel System Loop       |boolean|   	|   	|   	|
|Fuel System Status Code|category|   	|   	|   	|
|Long Term Trim 1       |%   	|   	|   	|   	|
|Short Term Trim 1      |%   	|   	|   	|   	|


# Vosk Model Adaptation

## Compile Kaldi

> A pre-requisite for model adaptation

Kaldi features a standard GNU Makefile based build system. Such a build system is traditionally achieved using the command `make`. The recipe to build a package is in a `Makefile`.

Windows installation guide can be found [here](https://github.com/kaldi-asr/kaldi/blob/master/windows/INSTALL.md).
We will discuss linux compilation and installation of kaldi here:

### Download Kaldi
Download and open the kaldi repository
```sh
git clone https://github.com/kaldi-asr/kaldi.git
cd kaldi
```

**Compiling Kaldi involves compilation of the following two folders `/tools` ad `/src`.**

#### 1. Compile `/tools`

- Enter `/tools`
    ```sh
    cd tools
    ```

- Check the pre-requisites for kaldi
    ```sh
    ./extras/check_dependencies.sh
    ```
    See if there are any system-level installations you need to do. Check the output carefully. There are some things that will make your life a lot easier if you fix them at this stage.<br>
    In `extras/`, there are also various scripts to install extra bits and pieces that are used by individual example scripts.  If an example script needs you to run one of those scripts, it will tell you what to do.
<br>
- Run `make`
    ```sh
    make -j 4
    ```

    You can do a parallel build by supplying the **"-j"** option to make, e.g. to use 4 CPUs -> -j 4, increase or decrease this according to your system specifications.

    It will take a long time if done with single CPU.

#### 2. Compile `/src`

You must first have completed the installation steps in `../tools/INSTALL` *(compiling OpenFst; getting ATLAS and CLAPACK headers)*.

- Configure
    ```sh
    ./configure --shared
    ```

- Run `make depend`
    ```sh
    make depend -j 8
    ```

- Run `make`
    ```sh
    make -j 8
    ```

And Done 🎉,
You can go ahead with the next steps.

## Update Vosk Model
There are many issues which can lead to bad accuracy from model mismatches to software bugs. See [accuracy](https://alphacephei.com/vosk/accuracy) guide for more detailed information on how to debug the accuracy problems. Once you figured out the reason is in the model mismatch you can try to update existing models to get better performance.

**Training a model from scratch is very rarely a great solution. In most cases you’d better adapt the existing model than train a new one.**

There are three levels of adaptation particularly important to us:
- Update LM Grammar *(New word addition ❌)*
- Update LM Vocabulary *(New word addition ✅)*
- Update Acoustic Model *(1 hour of custom data)*

### Pre-requisites
The following software must be pre-installed and configured on the server:
- Kaldi *(Compiled)*
- Phonetisaurus `pip3 install phonetisaurus`
- SRILM

> Note: Suggested is to perform the process as a **super user**, as the it requires elevated access at multiple locations. This will significantly decrease your burden in later steps.

### Update Language Model Grammar

Generally, if you don't need to add new words to your model, you most probably just need to adjust the probability of the words to improve the recognition. For that it is enough to recompile the language model with custom words.

#### 1. Prepare a text file

- Take a text that reflects the speech you want to recognize.
    ```txt
    EnviroCar listen!
    ```
- Remove punctuations.
    ```txt
    EnviroCar listen
    ```
- Convert everything to the lowercase, you can do it with a python script
    ```txt
    envirocar listen
    ```
- Save it as `text.txt` at:
    For small models
    ```sh
    <model folder>/graph
    ```
    For large models
    ```sh
    <model folder>/exp/chain/tdnn/graph
    ```

#### 2. Environment Setup

- Setup `KALDI_ROOT` env variable
    ```sh
    export KALDI_ROOT=<Kaldi-folder-path>
    ```
- Setup `PATH` env variable
    ```sh
    export PATH=$KALDI_ROOT/tools/openfst/bin:$PATH
    ```
- Setup `LD_LIBRARY_PATH` env variable
    ```sh
    export LD_LIBRARY_PATH=$KALDI_ROOT/tools/openfst/lib/fst
    ```

#### 3. Re-compile Grammar
Make sure you are on the same folder as the text file (as directed in step 1) 
- Generate word list
    ```sh
    fstsymbols --save_osymbols=words.txt Gr.fst > /dev/null
    ```
- Generate Gr.new.fst
    ```sh
    farcompilestrings --fst_type=compact --symbols=words.txt --keep_symbols text.txt | \
    ngramcount | ngrammake | \
    fstconvert --fst_type=ngram > Gr.new.fst
    ```
- Replace Gr.fst with Gr.new.fst
    ```sh
    mv Gr.new.fst Gr.fst
    ```

And Done 🎉,
Use the newly generated Gr.fst in the model.

### Update Language Model Vocabulary

This facilitates adding new words to the Vosk model.
Download the update package, for example
- [Russian](https://alphacephei.com/vosk/models/vosk-model-ru-0.22-compile.zip)

- [US English](https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-compile.zip)

- [German](https://alphacephei.com/vosk/models/vosk-model-de-0.21-compile.zip)

- [French](https://alphacephei.com/vosk/models/vosk-model-fr-0.6-linto-2.2.0-compile.zip)

Other language packs are available [here]().

**Download and unpack the model as a pre-requisite❗**

#### 1. Extend the dictionary 
If you want to recognize new words you need to add them to the dictionary. You can do it in many ways:

- Pick them from a larger hand-verified dictionary. There are few of them
- Use special machine learning library (usually called g2p which is grapheme to phoneme)
- Add them manually following the sounds of the word

Large hand-curated dictionaries are very rare, moreover, new words like “covid” and abbreviations appear all the time, so the first variant is not very practical.

The third one is also not recommended either as that is subjective and may vary person-to-person.

We will focus on the second option and more particularly on **[Phonetisaurus](https://github.com/rhasspy/phonetisaurus-pypi)** which is a simple WFST (HMM-based) toolkit that enables fast training and almost accurate prediction.

You must generate phonemes for every new word you are going to add.

- Create a new directory
    ```sh
    cd ~
    mkdir phonetisaurus-g2p
    ```

- Install `phonetisaurus`
    ```sh
    pip3 install phonetisaurus
    ```

- Copy and rename existing dictionary file *(en.dic)*
    ```sh
    cp <path-to-vosk-model>/db/en.dic $HOME/phonetisaurus-g2p/lexicon.dict
    ```

- Train the phonetisaurus model
    ```sh
    phonetisaurus train --model g2p.fst lexicon.dict
    ```

- Predict phonemes
    ```sh
    phonetisaurus predict --model g2p.fst word1 word2 ...
    ```

- Copy the results into `extra.dic` file<br>
    Vosk follows the [CMU pronouncing dictionary](https://github.com/cmusphinx/cmudict) format for dictionary files, you must follow the same format.
    ```
    word1 phoneme1 phoneme2 ...
    word2 phoneme1 phoneme2 phoneme3 ...
    ```

    Paste the content in `<path-to-vosk-model>/db/extra.dic` similar to:

    ```
    envirocar E n v aI r oU k A r
    ```
<br>

#### 2. Re-compile language model

Language model describes the probabilities of the sequences of words in the text and is required for speech recognition. Generic models are very large (several gigabytes and thus impractical). Most recognition systems have models tuned to the specific domain. For example, the medical language model describes medical dictation. If you are looking for your domain you most likely will have to build the language model for that domain yourself. You can mix that specific domain with generic domain to get some fallback, but specific domain is still needed. Generic language models are created from large texts.

- Get to the unpacked model directory
<br>
- Add your new words to `db/extra.txt` file. One word per line.
<br>
- Properly point the `KALDI_ROOT` in `path.sh` to the compiled Kaldi directory location ***(Very Important Step)***
<br>
- Run `compile-graph.sh`
    ```sh
    ./compile-graph.sh
    ```

    > ❗ Note: This might return some errors mostly related to permissions or dependencies even if you have followed everything right till now.
    If the error suggests any missing libraries that may to specific to your server, just install them.<br>
    A debugging approach is to re-check the following pointers:
     • Kaldi is compiled correctly and following the guide
     • Ubuntu/OS user has elevated or super user priviledges
     • Environment Variables are set-up and correct

    Update process will take time about 15-20 minutes and may extend further to 40-45 minutes for medium/low end systems.

- Run `decode.sh`
    ```sh
    ./decode.sh
    ```
    To test decoding works successfully. Watch the WER in the decoding folder.
    This will also take similar time or sometimes more time than the compilation step.

And, Done 🎉
For the compilation results read the next section on outputs.

#### 3. Outputs

Depending on your needs you might pick some result files from the compilation folder. Remember, that if you changed the graph you also need to change the rescoring/rnnlm part, otherwise they will go out of sync and accuracy will be low.

For large model pick the following parts:

- `exp/chain/tdnn/graph`
- `data/lang_test_rescore/G.fst` and `data/lang_test_rescore/G.carpa` into `rescore` folder
- `exp/rnnlm_out` into `rnnlm` folder, you can delete some unnecessary files from rnnlm too.
If you don’t want to use RNNLM, delete rnnlm folder from the model.

If you don’t want to use rescoring, delete the rescore folder from the model, that will save you some runtime memory, but accuracy will be lower.

**For small model, just pick the required files from `exp/chain/tdnn/lgraph`, viz the case for us.**

## Changelog

Check out the [Changelog](https://github.com/enviroCar/enviroCar-app/blob/master/CHANGELOG.md) for current changes.

## OBD simulator

The repository also contains a simple OBD simulator (dumb, nothing fancy) that can
be used on another Android device and mock the actual car adapter.

## References

This app is in operational use in the [CITRAM - Citizen Science for Traffic Management](https://www.citram.de/) project. Check out the [enviroCar website](https://envirocar.org/) for more information about the enviroCar project.

## How to Contribute
For contributing to the enviroCar Android App, please, have a look at our [Contributor Guidelines](https://github.com/enviroCar/enviroCar-app/blob/master/CONTRIBUTING.md).


## Contributors

Here is the list of [contributors to this project](https://github.com/enviroCar/enviroCar-app/blob/master/CONTRIBUTORS.md)
