# Choose your LLM

Elite Intel needs a language model (LLM) to understand your commands and answer your questions.
There are two ways to get one, and both are set up in **Services → AI Services → Language Model (LLM)**:

| Option | What you need | Cost |
|--------|---------------|------|
| **Local: LM Studio** | A capable GPU, [LM Studio](https://lmstudio.ai) and the `google/gemma-4-e4b` model | Free. Everything stays on your PC |
| **Cloud** | An account and an API key from a supported provider | The provider bills you for what you use |

![LLM setup](images/llm-setup.png)

---

## Option 1: Local (LM Studio)

The left side of the panel, **LMStudio Setup**:

| Field | Value |
|-------|-------|
| **Address** | `http://localhost:1234/v1/chat/completions`. If LM Studio runs on another PC on your network, put that PC's IP address in place of `localhost` |
| **Model** | `google/gemma-4-e4b` |

LM Studio is the only local inference server Elite Intel supports, and `google/gemma-4-e4b` is the
only supported local model. There are two ways to run it.

### On your gaming PC: Elite Intel sets it up

If your PC can run the game and the model together, a button appears under the **LMStudio Setup** fields.
Press **Install LM Studio**, and Elite Intel:

1. installs LM Studio, on the drive you choose,
2. downloads `google/gemma-4-e4b` (about 6 GB),
3. loads it onto your graphics card and checks that it really went there,
4. tests it, and fills in **Address** and **Model** for you.

Each step shows as it happens, with a progress bar for the download. If a step fails, the dialog says which
one and why, and pressing the button again picks up where it stopped. Allow about 10 GB of disk space.
LM Studio is free software from Element Labs, not part of Elite Intel.

From then on, Elite Intel starts LM Studio when you start its services and stops it when you stop them or
close the app, so the model uses your graphics card's memory only while Elite Intel is running. To remove it,
press **Uninstall LM Studio**: it lists what will be deleted before it deletes anything.

**The button appears only on an NVIDIA RTX 3090 (24 GB) or better.** The game takes 8.5 GB or more of video
memory and the model about 6 GB, and your browser, OBS and Windows use some too, so a smaller card does not
hold both reliably. AMD cards are not detected yet: AMD owners can follow the
[AMD guide](AMD-RX-7800XT-LLM-Setup).

Already have LM Studio? The button says **Set up LM Studio** instead. It adds the model and fills in the
fields, and leaves your LM Studio otherwise exactly as it is: Elite Intel never starts, stops or removes an
LM Studio you installed yourself.

### On a second PC: set it up yourself

If your gaming PC can't hold the game and the model together, run LM Studio on another PC on your network
that runs nothing but the model, and put that PC's address in **Address**. The gaming PC then does no
inference work.

The LLM PC needs:

- **12 GB of video memory**, which leaves headroom for the model,
- **RTX 3080-class processing power or better.** A slower card holds the model but answers too slowly to be
  useful.

A GPU reference table provided by **Kevin Rank** is available here:
[GPU Reference Guide](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

| Guide | |
|-------|---|
| [LM Studio - Linux](Install-LM-Studio-Linux) | Command-line install, plus running it as a service from boot |
| [LM Studio - Windows](Install-LM-Studio-Windows) | Command-line install, plus starting it at login |
| [LM Studio on Ubuntu Server](LM-Studio-Ubuntu-Server) | Headless install on a dedicated inference machine |
| [LLMs on AMD RX series](AMD-RX-7800XT-LLM-Setup) | AMD-specific setup |

### Why `google/gemma-4-e4b` specifically?

Elite Intel is a command parser and data analysis tool, not a conversational chatbot. Writing
natural-sounding banter is not enough. The model has to work out the right action from what you
said, analyse game data, and return structured results instead of an essay or HTML. Not every model
this size does that reliably.

The hard requirement is **function calling**. Elite Intel's companion does not ask the model to
describe what it would do. It gives the model a set of tools and expects it to call one, with
arguments. A model that cannot produce a well-formed tool call cannot drive the app at all, however
well it writes. `google/gemma-4-e4b` supports function calling.

At about 6.3 GB it fits in VRAM next to the game on a 24 GB card with room to spare. Nothing gets
offloaded to the CPU, so responses stay fast.

> **On the retired V1.0 model.** Earlier versions recommended `tulu-3.1-8b-supernova`. It does not
> support function calling, so it cannot run the companion and no longer works with Elite Intel. If
> you are following an older guide, ignore it and install `google/gemma-4-e4b`.

---

## Option 2: Cloud

The right side of the panel, **Cloud Setup**:

1. Pick your provider from the **Provider** list.
2. Paste that provider's key into **API Key**.
3. Tick **Locked** so the saved key can't be edited by accident.
4. Press **Save**, then start or restart services on the Vega tab.

You choose the provider yourself. Elite Intel does not try to guess it from the key, because
providers change their key formats too often for that to be reliable. You don't choose a model:
Elite Intel picks the right one for your provider.

| Provider | Notes |
|----------|-------|
| **Anthropic (Claude)** | Paid |
| **OpenAI** | Paid |
| **Google (Gemini)** | Paid |
| **xAI (Grok)** | Paid |
| **DeepSeek** | Paid |

How to get a key from each provider: [Cloud LLM setup](cloud-llm-options).

---

## Which one should I use?

**Run LM Studio locally if:**
- Your gaming PC has an NVIDIA RTX 3090 24 GB or better: Elite Intel sets it up for you
- Or you have a second PC on your network with 12 GB of video memory and RTX 3080-class power
- You want everything to stay on your own hardware

**Use a cloud provider if:**
- Your GPU can't run the model alongside the game, and you have no second PC for it
- You'd rather not run an inference server at all

Not sure? You can start with either one and switch any time without reinstalling.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
