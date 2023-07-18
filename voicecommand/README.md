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
    Please review the system-level installations to ensure there are no issues that need attention. Pay close attention to the output, as resolving any potential problems now will greatly save your time and efforts in tthe later process.<br>
    In the `extras/` directory, you'll find several scripts designed to install additional components utilized by specific example scripts. When an example script requires the execution of one of these scripts, it will provide clear instructions on what steps to take.
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
*There are many issues which can lead to bad accuracy from model mismatches to software bugs. See [accuracy](https://alphacephei.com/vosk/accuracy) guide for more detailed information on how to debug the accuracy problems. Once you figured out the reason is in the model mismatch you can try to update existing models to get better performance.*<sup>[[ref](https://alphacephei.com/vosk/adaptation)]</sup>

**In majority of cases, training a model from scratch is not the optimal solution. It is often more advantageous to adapt an existing model rather than creating a new one.**

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

Generally, if you don't need to add new words to your model, you most probably just need to adjust the probability of the words to improve the recognition. For that it is enough to recompile the language model with custom words.</sup>

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

Other language packs are available [here](https://alphacephei.com/vosk/models).

**Download and unpack the model as a pre-requisite❗**

#### 1. Extend the dictionary 
To enable the recognition of new words, they must be incorporated into the dictionary. This can be achieved through several methods:

- Select from a comprehensive, hand verified and reliable dictionary.
- Predict using Machine Learning g2p (grapheme to phoneme) tools like Phonetisaurus. **(Recommended)**
- Manually adding the words, following their sound patterns.

*Large hand-curated dictionaries are very rare, moreover, new words like “covid” and abbreviations appear all the time, so the first variant is not very practical.*<sup>[[ref](https://alphacephei.com/vosk/lm)]</sup>

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

*Language model describes the probabilities of the sequences of words in the text and is required for speech recognition. Generic models are very large (several gigabytes and thus impractical). Most recognition systems have models tuned to the specific domain. For example, the medical language model describes medical dictation. If you are looking for your domain you most likely will have to build the language model for that domain yourself. You can mix that specific domain with generic domain to get some fallback, but specific domain is still needed. Generic language models are created from large texts.*<sup>[[ref](https://alphacephei.com/vosk/lm)]</sup>
<br>

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

#### 3. Outputs<sup>[[ref](https://alphacephei.com/vosk/lm)]</sup>

Depending on your needs you might pick some result files from the compilation folder. Remember, that if you changed the graph you also need to change the rescoring/rnnlm part, otherwise they will go out of sync and accuracy will be low.

For large model pick the following parts:

- `exp/chain/tdnn/graph`
- `data/lang_test_rescore/G.fst` and `data/lang_test_rescore/G.carpa` into `rescore` folder
- `exp/rnnlm_out` into `rnnlm` folder, you can delete some unnecessary files from rnnlm too.
If you don’t want to use RNNLM, delete rnnlm folder from the model.

If you don’t want to use rescoring, delete the rescore folder from the model, that will save you some runtime memory, but accuracy will be lower.

**For small model, just pick the required files from `exp/chain/tdnn/lgraph`, viz the case for us.**

## References

- https://alphacephei.com/vosk/adaptation
- https://alphacephei.com/vosk/lm
- https://github.com/rhasspy/phonetisaurus-pypi#readme
- https://kaldi-asr.org/doc/build_setup.html
- http://www.speech.sri.com/projects/srilm/download.html
- https://alphacephei.com/vosk/models