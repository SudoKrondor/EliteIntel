#!/usr/bin/env python3
from pypinyin import phrases_dict, load_phrases_dict

user_dict = {
    "还田": [["huan2"], ["tian2"]],
    "行长": [["hang2"], ["zhang3"]],
    "银行行长": [["yin2"], ["hang2"], ["hang2"], ["zhang3"]],
}

load_phrases_dict(user_dict)

phrases_dict.phrases_dict.update(**user_dict)


def main():
    phrases = phrases_dict.phrases_dict

    with open("./user.dict.utf8", "w", encoding="utf-8") as f:
        for phrase in phrases:
            f.write(f"{phrase} 10000 v\n")


if __name__ == "__main__":
    main()
