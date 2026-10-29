#!/usr/bin/env python3
"""Generates the bundled SAMPLE question bank (seed/questions.json).

The data imitates the shape of real SSC/HSC board papers (all boards, all years, MCQ and
creative questions with ক/খ/গ/ঘ parts) so the app can be built and tested end to end.
It is NOT an official board paper collection. Replace it with verified content before release.

Run:  python3 tools/sample_seed/generate.py
"""
import json
import math
import random
from pathlib import Path

from pools_hsc import POOLS_HSC
from pools_ssc import POOLS_SSC

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "seed" / "questions.json"
SEED_VERSION = 3

BN_DIGITS = str.maketrans("0123456789", "০১২৩৪৫৬৭৮৯")


def bn(value) -> str:
    return str(value).replace("-", "−").translate(BN_DIGITS)


def fmt(x) -> str:
    """Formats a number for Bangla text, dropping a trailing .0."""
    if isinstance(x, float):
        x = round(x, 2)
        if x == int(x):
            x = int(x)
    return bn(x)


# ----------------------------------------------------------------------------- catalog

EXAMS = [
    {"id": "SSC", "name": "এসএসসি", "fullName": "মাধ্যমিক স্কুল সার্টিফিকেট"},
    {"id": "HSC", "name": "এইচএসসি", "fullName": "উচ্চ মাধ্যমিক সার্টিফিকেট"},
]

BOARDS = [
    ("dhaka", "ঢাকা", "Dhaka"),
    ("rajshahi", "রাজশাহী", "Rajshahi"),
    ("chattogram", "চট্টগ্রাম", "Chattogram"),
    ("cumilla", "কুমিল্লা", "Cumilla"),
    ("jashore", "যশোর", "Jashore"),
    ("barishal", "বরিশাল", "Barishal"),
    ("sylhet", "সিলেট", "Sylhet"),
    ("dinajpur", "দিনাজপুর", "Dinajpur"),
    ("mymensingh", "ময়মনসিংহ", "Mymensingh"),
    ("madrasah", "মাদ্রাসা", "Madrasah"),
    ("technical", "কারিগরি", "Technical"),
]
MYMENSINGH_FIRST_YEAR = 2018
YEARS = list(range(2015, 2026))

# Years where only group (elective) subjects were examined, or no exam was held.
NOTES = [
    {"exam": "HSC", "year": 2020, "text": "করোনা মহামারির কারণে ২০২০ সালের এইচএসসি পরীক্ষা অনুষ্ঠিত হয়নি।"},
    {"exam": "SSC", "year": 2021, "text": "২০২১ সালে সংক্ষিপ্ত সিলেবাসে শুধু গ্রুপভিত্তিক বিষয়ের পরীক্ষা হয়েছিল।"},
    {"exam": "HSC", "year": 2021, "text": "২০২১ সালে সংক্ষিপ্ত সিলেবাসে শুধু গ্রুপভিত্তিক বিষয়ের পরীক্ষা হয়েছিল।"},
]

SUBJECTS = [
    # id, exam, name, nameEn, code, group, chapters
    # ---------------------------------------------------------------- SSC
    ("ssc-bangla-1", "SSC", "বাংলা ১ম পত্র", "Bangla 1st Paper", "101", "COMMON", ["গদ্য", "কবিতা", "সহপাঠ: উপন্যাস ও নাটক"]),
    ("ssc-bangla-2", "SSC", "বাংলা ২য় পত্র", "Bangla 2nd Paper", "102", "COMMON", ["ধ্বনি ও বর্ণ", "সন্ধি ও সমাস", "বাক্য ও প্রয়োগ"]),
    ("ssc-english-1", "SSC", "English 1st Paper", "English 1st Paper", "107", "COMMON", ["Reading Comprehension", "Vocabulary", "Writing Skills"]),
    ("ssc-english-2", "SSC", "English 2nd Paper", "English 2nd Paper", "108", "COMMON", ["Articles and Prepositions", "Tense and Voice", "Narration and Sentences"]),
    ("ssc-math", "SSC", "গণিত", "General Mathematics", "109", "COMMON", ["বাস্তব সংখ্যা", "সেট ও ফাংশন", "বীজগাণিতিক রাশি"]),
    ("ssc-bgs", "SSC", "বাংলাদেশ ও বিশ্বপরিচয়", "Bangladesh and Global Studies", "150", "COMMON",
     ["মুক্তিযুদ্ধ ও বাংলাদেশের অভ্যুদয়", "বাংলাদেশের সংবিধান", "বাংলাদেশ ও আন্তর্জাতিক সহযোগিতা"]),
    ("ssc-ict", "SSC", "তথ্য ও যোগাযোগ প্রযুক্তি", "ICT", "154", "COMMON",
     ["তথ্য ও যোগাযোগ প্রযুক্তি ও আমাদের বাংলাদেশ", "কম্পিউটার ও কম্পিউটার ব্যবহারকারীর নিরাপত্তা", "আমার লেখালেখি ও হিসাব"]),
    ("ssc-islam", "SSC", "ইসলাম ও নৈতিক শিক্ষা", "Islam and Moral Education", "111", "COMMON", ["আকাইদ ও নৈতিক জীবন", "ইবাদত", "আখলাক"]),
    ("ssc-hindu", "SSC", "হিন্দুধর্ম ও নৈতিক শিক্ষা", "Hinduism and Moral Education", "112", "COMMON", ["স্রষ্টা ও সৃষ্টি", "ধর্মগ্রন্থ", "নৈতিক মূল্যবোধ"]),
    ("ssc-physics", "SSC", "পদার্থবিজ্ঞান", "Physics", "136", "SCIENCE", ["ভৌত রাশি এবং পরিমাপ", "গতি", "বল"]),
    ("ssc-chemistry", "SSC", "রসায়ন", "Chemistry", "137", "SCIENCE", ["রসায়নের ধারণা", "পদার্থের অবস্থা", "পদার্থের গঠন"]),
    ("ssc-biology", "SSC", "জীববিজ্ঞান", "Biology", "138", "SCIENCE", ["জীবন পাঠ", "জীবকোষ ও টিস্যু", "কোষ বিভাজন"]),
    ("ssc-highermath", "SSC", "উচ্চতর গণিত", "Higher Mathematics", "126", "SCIENCE", ["বীজগাণিতিক রাশি", "ত্রিকোণমিতিক অনুপাত", "অনুক্রম ও ধারা"]),
    ("ssc-accounting", "SSC", "হিসাববিজ্ঞান", "Accounting", "146", "BUSINESS", ["হিসাববিজ্ঞান পরিচিতি", "লেনদেন", "দুতরফা দাখিলা পদ্ধতি"]),
    ("ssc-finance", "SSC", "ফিন্যান্স ও ব্যাংকিং", "Finance and Banking", "152", "BUSINESS", ["অর্থায়নের ধারণা", "অর্থের সময়মূল্য", "ব্যাংক ব্যবস্থা"]),
    ("ssc-business", "SSC", "ব্যবসায় উদ্যোগ", "Business Entrepreneurship", "143", "BUSINESS", ["ব্যবসায় পরিচিতি", "ব্যবসায় উদ্যোগ ও উদ্যোক্তা", "ব্যবসায়ের আইনগত দিক"]),
    ("ssc-geography", "SSC", "ভূগোল ও পরিবেশ", "Geography and Environment", "110", "HUMANITIES", ["ভূগোল ও পরিবেশ", "মহাবিশ্ব ও আমাদের পৃথিবী", "বাংলাদেশের ভৌগোলিক বিবরণ"]),
    ("ssc-economics", "SSC", "অর্থনীতি", "Economics", "141", "HUMANITIES", ["অর্থনীতি পরিচয়", "চাহিদা, যোগান ও ভারসাম্য", "জাতীয় আয়"]),
    ("ssc-civics", "SSC", "পৌরনীতি ও নাগরিকতা", "Civics and Citizenship", "140", "HUMANITIES", ["পৌরনীতি ও নাগরিকতা", "রাষ্ট্র ও সরকার", "নাগরিক অধিকার ও কর্তব্য"]),
    ("ssc-history", "SSC", "বাংলাদেশের ইতিহাস ও বিশ্বসভ্যতা", "History of Bangladesh and World Civilization", "153", "HUMANITIES",
     ["ইতিহাস পরিচিতি", "বিশ্বসভ্যতা", "প্রাচীন বাংলার জনপদ"]),
    ("ssc-science", "SSC", "বিজ্ঞান", "Science", "127", "OPTIONAL", ["খাদ্য ও পুষ্টি", "রোগ প্রতিরোধ ও স্বাস্থ্য", "মানবদেহ"]),
    ("ssc-agriculture", "SSC", "কৃষিশিক্ষা", "Agriculture Studies", "134", "OPTIONAL", ["কৃষি প্রযুক্তি", "কৃষি উপকরণ", "কৃষি ও জলবায়ু"]),
    ("ssc-homescience", "SSC", "গার্হস্থ্য বিজ্ঞান", "Home Science", "151", "OPTIONAL", ["গৃহ ব্যবস্থাপনা", "খাদ্য ও পুষ্টি", "শিশুর বিকাশ"]),
    # ---------------------------------------------------------------- HSC
    ("hsc-bangla-1", "HSC", "বাংলা ১ম পত্র", "Bangla 1st Paper", "101", "COMMON", ["গদ্য", "কবিতা", "উপন্যাস ও নাটক"]),
    ("hsc-bangla-2", "HSC", "বাংলা ২য় পত্র", "Bangla 2nd Paper", "102", "COMMON", ["বাংলা উচ্চারণের নিয়ম", "বাংলা বানানের নিয়ম", "বাক্যশুদ্ধি"]),
    ("hsc-english-1", "HSC", "English 1st Paper", "English 1st Paper", "107", "COMMON", ["Reading Comprehension", "Vocabulary", "Writing Skills"]),
    ("hsc-english-2", "HSC", "English 2nd Paper", "English 2nd Paper", "108", "COMMON", ["Linking Words and Clauses", "Changing Parts of Speech", "Tag Questions"]),
    ("hsc-ict", "HSC", "তথ্য ও যোগাযোগ প্রযুক্তি", "ICT", "275", "COMMON",
     ["বিশ্ব ও বাংলাদেশ প্রেক্ষিত", "কমিউনিকেশন সিস্টেমস ও নেটওয়ার্কিং", "সংখ্যা পদ্ধতি ও ডিজিটাল ডিভাইস"]),
    ("hsc-physics-1", "HSC", "পদার্থবিজ্ঞান ১ম পত্র", "Physics 1st Paper", "174", "SCIENCE", ["ভৌতজগৎ ও পরিমাপ", "ভেক্টর", "গতিবিদ্যা"]),
    ("hsc-physics-2", "HSC", "পদার্থবিজ্ঞান ২য় পত্র", "Physics 2nd Paper", "175", "SCIENCE", ["তাপগতিবিদ্যা", "স্থির তড়িৎ", "চল তড়িৎ"]),
    ("hsc-chemistry-1", "HSC", "রসায়ন ১ম পত্র", "Chemistry 1st Paper", "176", "SCIENCE",
     ["ল্যাবরেটরির নিরাপদ ব্যবহার", "গুণগত রসায়ন", "মৌলের পর্যায়বৃত্ত ধর্ম ও রাসায়নিক বন্ধন"]),
    ("hsc-chemistry-2", "HSC", "রসায়ন ২য় পত্র", "Chemistry 2nd Paper", "177", "SCIENCE", ["পরিবেশ রসায়ন", "জৈব রসায়ন", "পরিমাণগত রসায়ন"]),
    ("hsc-biology-1", "HSC", "জীববিজ্ঞান ১ম পত্র", "Biology 1st Paper", "178", "SCIENCE", ["কোষ ও এর গঠন", "কোষ বিভাজন", "কোষ রসায়ন"]),
    ("hsc-biology-2", "HSC", "জীববিজ্ঞান ২য় পত্র", "Biology 2nd Paper", "179", "SCIENCE", ["প্রাণীর বিভিন্নতা ও শ্রেণিবিন্যাস", "মানব শারীরতত্ত্ব: পরিপাক ও শোষণ", "মানব শারীরতত্ত্ব: রক্ত ও সঞ্চালন"]),
    ("hsc-highermath-1", "HSC", "উচ্চতর গণিত ১ম পত্র", "Higher Mathematics 1st Paper", "265", "SCIENCE", ["ম্যাট্রিক্স ও নির্ণায়ক", "ভেক্টর", "সরলরেখা"]),
    ("hsc-highermath-2", "HSC", "উচ্চতর গণিত ২য় পত্র", "Higher Mathematics 2nd Paper", "266", "SCIENCE", ["বাস্তব সংখ্যা ও অসমতা", "জটিল সংখ্যা", "দ্বিপদী বিস্তৃতি"]),
    ("hsc-accounting-1", "HSC", "হিসাববিজ্ঞান ১ম পত্র", "Accounting 1st Paper", "253", "BUSINESS", ["হিসাববিজ্ঞান পরিচিতি", "হিসাবের বইসমূহ", "ব্যাংক সমন্বয় বিবরণী"]),
    ("hsc-accounting-2", "HSC", "হিসাববিজ্ঞান ২য় পত্র", "Accounting 2nd Paper", "254", "BUSINESS", ["অংশীদারি ব্যবসায়ের হিসাব", "আর্থিক বিবরণী", "আর্থিক বিবরণী বিশ্লেষণ"]),
    ("hsc-management-1", "HSC", "ব্যবসায় সংগঠন ও ব্যবস্থাপনা ১ম পত্র", "Business Organization and Management 1st Paper", "277", "BUSINESS",
     ["ব্যবসায়ের মৌলিক ধারণা", "ব্যবসায়ের পরিবেশ", "একমালিকানা ও অংশীদারি ব্যবসায়"]),
    ("hsc-management-2", "HSC", "ব্যবসায় সংগঠন ও ব্যবস্থাপনা ২য় পত্র", "Business Organization and Management 2nd Paper", "278", "BUSINESS",
     ["ব্যবস্থাপনার ধারণা", "পরিকল্পনা ও সংগঠন", "নেতৃত্ব ও প্রেষণা"]),
    ("hsc-finance-1", "HSC", "ফিন্যান্স, ব্যাংকিং ও বিমা ১ম পত্র", "Finance, Banking and Insurance 1st Paper", "292", "BUSINESS",
     ["ফিন্যান্স পরিচিতি", "অর্থের সময়মূল্য", "মূলধন বাজেটিং"]),
    ("hsc-finance-2", "HSC", "ফিন্যান্স, ব্যাংকিং ও বিমা ২য় পত্র", "Finance, Banking and Insurance 2nd Paper", "293", "BUSINESS",
     ["ব্যাংক ব্যবস্থা", "কেন্দ্রীয় ব্যাংক", "বিমা"]),
    ("hsc-marketing-1", "HSC", "উৎপাদন ব্যবস্থাপনা ও বিপণন ১ম পত্র", "Production Management and Marketing 1st Paper", "286", "BUSINESS",
     ["উৎপাদন", "উৎপাদনশীলতা", "ব্যয় ও সমচ্ছেদ বিশ্লেষণ"]),
    ("hsc-marketing-2", "HSC", "উৎপাদন ব্যবস্থাপনা ও বিপণন ২য় পত্র", "Production Management and Marketing 2nd Paper", "287", "BUSINESS",
     ["বিপণন পরিচিতি", "বিপণন মিশ্রণ", "বিজ্ঞাপন ও বিক্রয় প্রসার"]),
    ("hsc-economics-1", "HSC", "অর্থনীতি ১ম পত্র", "Economics 1st Paper", "109", "HUMANITIES", ["মৌলিক অর্থনৈতিক সমস্যা", "ভোক্তা ও উৎপাদকের আচরণ", "চাহিদা ও যোগান"]),
    ("hsc-economics-2", "HSC", "অর্থনীতি ২য় পত্র", "Economics 2nd Paper", "110", "HUMANITIES", ["বাংলাদেশের অর্থনীতি", "বাংলাদেশের কৃষি", "বাংলাদেশের শিল্প ও বাণিজ্য"]),
    ("hsc-civics-1", "HSC", "পৌরনীতি ও সুশাসন ১ম পত্র", "Civics and Good Governance 1st Paper", "269", "HUMANITIES",
     ["পৌরনীতি ও সুশাসন", "রাষ্ট্র ও সরকার", "আইন, স্বাধীনতা ও সাম্য"]),
    ("hsc-civics-2", "HSC", "পৌরনীতি ও সুশাসন ২য় পত্র", "Civics and Good Governance 2nd Paper", "270", "HUMANITIES",
     ["ব্রিটিশ শাসন ও স্বাধিকার আন্দোলন", "ভাষা আন্দোলন থেকে মুক্তিযুদ্ধ", "বাংলাদেশের সংবিধান"]),
    ("hsc-history-1", "HSC", "ইতিহাস ১ম পত্র", "History 1st Paper", "304", "HUMANITIES", ["প্রাচীন বাংলা", "মধ্যযুগের বাংলা", "ব্রিটিশ শাসন"]),
    ("hsc-history-2", "HSC", "ইতিহাস ২য় পত্র", "History 2nd Paper", "305", "HUMANITIES", ["ইউরোপের রেনেসাঁস", "ফরাসি বিপ্লব", "বিশ্বযুদ্ধ"]),
    ("hsc-islamic-history-1", "HSC", "ইসলামের ইতিহাস ও সংস্কৃতি ১ম পত্র", "Islamic History and Culture 1st Paper", "267", "HUMANITIES",
     ["প্রাক-ইসলামি আরব", "মহানবি (সা.)-এর জীবন", "খোলাফায়ে রাশেদিন"]),
    ("hsc-islamic-history-2", "HSC", "ইসলামের ইতিহাস ও সংস্কৃতি ২য় পত্র", "Islamic History and Culture 2nd Paper", "268", "HUMANITIES",
     ["উমাইয়া খিলাফত", "আব্বাসীয় খিলাফত", "বাংলায় মুসলিম শাসন"]),
    ("hsc-logic-1", "HSC", "যুক্তিবিদ্যা ১ম পত্র", "Logic 1st Paper", "121", "HUMANITIES", ["যুক্তিবিদ্যার পরিচয়", "পদ ও বচন", "অনুমান"]),
    ("hsc-logic-2", "HSC", "যুক্তিবিদ্যা ২য় পত্র", "Logic 2nd Paper", "122", "HUMANITIES", ["আরোহ অনুমান", "কার্যকারণ ও পরীক্ষণ পদ্ধতি", "প্রকল্প ও ব্যাখ্যা"]),
    ("hsc-sociology-1", "HSC", "সমাজবিজ্ঞান ১ম পত্র", "Sociology 1st Paper", "117", "HUMANITIES", ["সমাজবিজ্ঞানের উৎপত্তি ও বিকাশ", "সমাজ ও সংস্কৃতি", "সামাজিকীকরণ"]),
    ("hsc-sociology-2", "HSC", "সমাজবিজ্ঞান ২য় পত্র", "Sociology 2nd Paper", "118", "HUMANITIES", ["বাংলাদেশের সমাজ ও সংস্কৃতি", "সামাজিক সমস্যা", "সামাজিক পরিবর্তন"]),
    ("hsc-geography-1", "HSC", "ভূগোল ১ম পত্র", "Geography 1st Paper", "125", "HUMANITIES", ["প্রাকৃতিক ভূগোল", "শিলা ও ভূমিরূপ", "বায়ুমণ্ডল ও জলবায়ু"]),
    ("hsc-geography-2", "HSC", "ভূগোল ২য় পত্র", "Geography 2nd Paper", "126", "HUMANITIES", ["জনসংখ্যা", "বসতি ও নগরায়ণ", "বাংলাদেশের সম্পদ"]),
    ("hsc-socialwork-1", "HSC", "সমাজকর্ম ১ম পত্র", "Social Work 1st Paper", "271", "HUMANITIES", ["সমাজকর্মের ধারণা", "সমাজকর্মের পদ্ধতি", "সমাজকল্যাণের ইতিহাস"]),
    ("hsc-socialwork-2", "HSC", "সমাজকর্ম ২য় পত্র", "Social Work 2nd Paper", "272", "HUMANITIES", ["বাংলাদেশে এনজিও", "সামাজিক সমস্যা", "সামাজিক নিরাপত্তা"]),
    ("hsc-psychology-1", "HSC", "মনোবিজ্ঞান ১ম পত্র", "Psychology 1st Paper", "123", "OPTIONAL", ["মনোবিজ্ঞানের পরিচিতি", "শিক্ষণ", "প্রেষণা ও আবেগ"]),
    ("hsc-psychology-2", "HSC", "মনোবিজ্ঞান ২য় পত্র", "Psychology 2nd Paper", "124", "OPTIONAL", ["বুদ্ধি", "ব্যক্তিত্ব", "মানসিক স্বাস্থ্য"]),
    ("hsc-statistics-1", "HSC", "পরিসংখ্যান ১ম পত্র", "Statistics 1st Paper", "129", "OPTIONAL", ["বিস্তার পরিমাপ", "কেন্দ্রীয় প্রবণতা: গড়", "কেন্দ্রীয় প্রবণতা: মধ্যমা"]),
    ("hsc-statistics-2", "HSC", "পরিসংখ্যান ২য় পত্র", "Statistics 2nd Paper", "130", "OPTIONAL", ["সম্ভাবনা", "নমুনাক্ষেত্র", "বিন্যাস ও সমাবেশ"]),
    ("hsc-agriculture-1", "HSC", "কৃষিশিক্ষা ১ম পত্র", "Agriculture Studies 1st Paper", "239", "OPTIONAL", ["কৃষি প্রযুক্তি", "মাটি ও সার", "সেচ ও ফসল ব্যবস্থাপনা"]),
    ("hsc-agriculture-2", "HSC", "কৃষিশিক্ষা ২য় পত্র", "Agriculture Studies 2nd Paper", "240", "OPTIONAL", ["পশুপালন", "মৎস্য চাষ", "কৃষি সমবায় ও বিপণন"]),
]

MCQ_PER_PAPER = 6
CQ_PER_PAPER = 2
REPEAT_SHARE = 0.25  # share of MCQs drawn from the "important" pool, so they repeat across boards


# ----------------------------------------------------------------------------- helpers

class Retry(Exception):
    """Raised when a generated question is unusable (e.g. not enough distinct options)."""


def mcq(chapter, prompt, correct, distractors, explanation, rng=None):
    unique = []
    for d in (str(x) for x in distractors):
        if d != str(correct) and d not in unique:
            unique.append(d)
    if len(unique) < 3:
        raise Retry(prompt)
    options = [str(correct)] + unique[:3]
    order = list(range(len(options)))
    (rng or random).shuffle(order)
    labels = ["ক", "খ", "গ", "ঘ"]
    shuffled = [options[i] for i in order]
    correct_label = labels[order.index(0)]
    return {
        "type": "MCQ",
        "chapter": chapter,
        "prompt": prompt,
        "options": [{"id": labels[i], "text": t} for i, t in enumerate(shuffled)],
        "correctOptionId": correct_label,
        "explanation": explanation,
    }


def cq(chapter, stem, parts):
    labels = ["ক", "খ", "গ", "ঘ"]
    marks = [1, 2, 3, 4]
    return {
        "type": "CQ",
        "chapter": chapter,
        "stem": stem,
        "parts": [
            {"label": labels[i], "text": text, "marks": marks[i], "answer": answer}
            for i, (text, answer) in enumerate(parts)
        ],
    }


def near(rng, answer, count=3, spread=None, positive=True, integer=True):
    """Plausible numeric distractors around `answer`."""
    spread = spread or max(2, abs(answer) // 3 if integer else abs(answer) / 3)
    out = set()
    guard = 0
    while len(out) < count and guard < 200:
        guard += 1
        delta = rng.randint(1, int(max(2, spread))) * rng.choice([-1, 1]) if integer \
            else round(rng.uniform(0.2, 1.0) * spread * rng.choice([-1, 1]), 2)
        cand = answer + delta
        if positive and cand <= 0:
            continue
        if cand != answer:
            out.add(cand)
    return list(out)


def pick_others(rng, pool, correct, count=3):
    others = [p for p in pool if p != correct]
    return rng.sample(others, count)


# ----------------------------------------------------------------------------- facts

ELEMENTS = [
    # Z, symbol, name, mass number
    (1, "H", "হাইড্রোজেন", 1), (2, "He", "হিলিয়াম", 4), (3, "Li", "লিথিয়াম", 7),
    (4, "Be", "বেরিলিয়াম", 9), (5, "B", "বোরন", 11), (6, "C", "কার্বন", 12),
    (7, "N", "নাইট্রোজেন", 14), (8, "O", "অক্সিজেন", 16), (9, "F", "ফ্লোরিন", 19),
    (10, "Ne", "নিয়ন", 20), (11, "Na", "সোডিয়াম", 23), (12, "Mg", "ম্যাগনেসিয়াম", 24),
    (13, "Al", "অ্যালুমিনিয়াম", 27), (14, "Si", "সিলিকন", 28), (15, "P", "ফসফরাস", 31),
    (16, "S", "সালফার", 32), (17, "Cl", "ক্লোরিন", 35), (18, "Ar", "আর্গন", 40),
    (19, "K", "পটাসিয়াম", 39), (20, "Ca", "ক্যালসিয়াম", 40), (24, "Cr", "ক্রোমিয়াম", 52),
    (26, "Fe", "আয়রন", 56), (29, "Cu", "কপার", 64), (30, "Zn", "জিংক", 65),
]

SUBSHELL_ORDER = ["1s", "2s", "2p", "3s", "3p", "4s", "3d", "4p"]
SUBSHELL_CAP = {"s": 2, "p": 6, "d": 10}
CONFIG_EXCEPTIONS = {24: "1s² 2s² 2p⁶ 3s² 3p⁶ 3d⁵ 4s¹", 29: "1s² 2s² 2p⁶ 3s² 3p⁶ 3d¹⁰ 4s¹"}
SUPER = str.maketrans("0123456789", "⁰¹²³⁴⁵⁶⁷⁸⁹")


def electron_config(z):
    if z in CONFIG_EXCEPTIONS:
        return CONFIG_EXCEPTIONS[z]
    left, parts = z, []
    for sub in SUBSHELL_ORDER:
        if left <= 0:
            break
        n = min(left, SUBSHELL_CAP[sub[-1]])
        parts.append(sub + str(n).translate(SUPER))
        left -= n
    return " ".join(parts)


def shell_counts(z):
    shells, left = [], z
    for cap in (2, 8, 8, 18):
        n = min(left, cap)
        if n <= 0:
            break
        shells.append(n)
        left -= n
    return shells


SCIENTIFIC_NAMES = [
    ("মানুষ", "Homo sapiens"), ("ধান", "Oryza sativa"), ("আম", "Mangifera indica"),
    ("কাঁঠাল", "Artocarpus heterophyllus"), ("রুই মাছ", "Labeo rohita"),
    ("ইলিশ মাছ", "Tenualosa ilisha"), ("রয়েল বেঙ্গল টাইগার", "Panthera tigris"),
    ("দোয়েল", "Copsychus saularis"), ("শাপলা", "Nymphaea nouchali"),
    ("পাট", "Corchorus capsularis"), ("গম", "Triticum aestivum"), ("জবা", "Hibiscus rosa-sinensis"),
]

ORGANELLES = [
    ("মাইটোকন্ড্রিয়া", "কোষের শক্তিঘর হিসেবে শক্তি উৎপাদন"),
    ("রাইবোসোম", "প্রোটিন সংশ্লেষণ"),
    ("ক্লোরোপ্লাস্ট", "সালোকসংশ্লেষণ"),
    ("লাইসোসোম", "অন্তঃকোষীয় পরিপাক"),
    ("গলগি বস্তু", "কোষের বিভিন্ন পদার্থ নিঃসরণ ও পরিবহন"),
    ("নিউক্লিয়াস", "কোষের সকল কাজ নিয়ন্ত্রণ"),
    ("সেন্ট্রোসোম", "প্রাণিকোষে কোষ বিভাজনে মাকুতন্তু গঠন"),
    ("কোষপ্রাচীর", "উদ্ভিদকোষকে দৃঢ়তা ও সুরক্ষা প্রদান"),
]

CHROMOSOMES = [
    ("মানুষ", 46), ("ধান", 24), ("পেঁয়াজ", 16), ("মটর", 14), ("ভুট্টা", 20),
    ("গম", 42), ("কুকুর", 78), ("বিড়াল", 38), ("ড্রসোফিলা", 8),
]

LIBERATION_FACTS = [
    ("বাংলাদেশের মহান স্বাধীনতা দিবস কোনটি?", "২৬ মার্চ", ["১৬ ডিসেম্বর", "২১ ফেব্রুয়ারি", "১৭ এপ্রিল"],
     "১৯৭১ সালের ২৬ মার্চ বাংলাদেশের স্বাধীনতা ঘোষণা করা হয়।"),
    ("বাংলাদেশের বিজয় দিবস কোনটি?", "১৬ ডিসেম্বর", ["২৬ মার্চ", "১৪ ডিসেম্বর", "৭ মার্চ"],
     "১৯৭১ সালের ১৬ ডিসেম্বর পাকিস্তানি বাহিনী আত্মসমর্পণ করে।"),
    ("মুজিবনগর সরকার শপথ গ্রহণ করে কবে?", "১৭ এপ্রিল ১৯৭১", ["১০ এপ্রিল ১৯৭১", "২৬ মার্চ ১৯৭১", "১৬ ডিসেম্বর ১৯৭১"],
     "১০ এপ্রিল সরকার গঠিত হয় এবং ১৭ এপ্রিল মেহেরপুরের বৈদ্যনাথতলায় শপথ গ্রহণ করে।"),
    ("মুক্তিযুদ্ধের সময় বাংলাদেশকে কয়টি সেক্টরে ভাগ করা হয়েছিল?", "১১টি", ["৮টি", "১০টি", "১২টি"],
     "যুদ্ধ পরিচালনার সুবিধার্থে সমগ্র দেশকে ১১টি সেক্টরে ভাগ করা হয়।"),
    ("ভাষা আন্দোলন সংঘটিত হয় কোন সালে?", "১৯৫২", ["১৯৪৮", "১৯৫৪", "১৯৬৬"],
     "১৯৫২ সালের ২১ ফেব্রুয়ারি রাষ্ট্রভাষা বাংলার দাবিতে আন্দোলন চূড়ান্ত রূপ নেয়।"),
    ("ঐতিহাসিক ছয় দফা ঘোষণা করা হয় কোন সালে?", "১৯৬৬", ["১৯৬২", "১৯৬৯", "১৯৭০"],
     "১৯৬৬ সালে বাঙালির স্বাধিকারের দাবিতে ছয় দফা উত্থাপন করা হয়।"),
]
CONSTITUTION_FACTS = [
    ("বাংলাদেশের সংবিধান গণপরিষদে গৃহীত হয় কবে?", "৪ নভেম্বর ১৯৭২", ["১৬ ডিসেম্বর ১৯৭২", "২৬ মার্চ ১৯৭২", "১০ এপ্রিল ১৯৭২"],
     "সংবিধান ৪ নভেম্বর ১৯৭২ গৃহীত হয় এবং ১৬ ডিসেম্বর ১৯৭২ থেকে কার্যকর হয়।"),
    ("বাংলাদেশের সংবিধান কার্যকর হয় কবে?", "১৬ ডিসেম্বর ১৯৭২", ["৪ নভেম্বর ১৯৭২", "২৬ মার্চ ১৯৭৩", "১ জানুয়ারি ১৯৭৩"],
     "বিজয়ের প্রথম বার্ষিকীতে সংবিধান কার্যকর করা হয়।"),
    ("বাংলাদেশের সংবিধানের মূলনীতি কয়টি?", "৪টি", ["৩টি", "৫টি", "৬টি"],
     "জাতীয়তাবাদ, সমাজতন্ত্র, গণতন্ত্র ও ধর্মনিরপেক্ষতা: এই চারটি মূলনীতি।"),
    ("বাংলাদেশের সংবিধান অনুযায়ী রাষ্ট্রের সর্বোচ্চ আইন কোনটি?", "সংবিধান", ["সংসদের আইন", "অধ্যাদেশ", "বিধিমালা"],
     "সংবিধান রাষ্ট্রের সর্বোচ্চ আইন; এর সাথে অসামঞ্জস্যপূর্ণ আইন বাতিল হয়।"),
]
INTERNATIONAL_FACTS = [
    ("বাংলাদেশ জাতিসংঘের সদস্যপদ লাভ করে কবে?", "১৭ সেপ্টেম্বর ১৯৭৪", ["১৬ ডিসেম্বর ১৯৭২", "২৬ মার্চ ১৯৭৩", "১০ জানুয়ারি ১৯৭৫"],
     "বাংলাদেশ জাতিসংঘের ১৩৬তম সদস্য।"),
    ("সার্কের সদর দপ্তর কোথায়?", "কাঠমান্ডু", ["ঢাকা", "নয়াদিল্লি", "কলম্বো"],
     "সার্ক সচিবালয় নেপালের কাঠমান্ডুতে অবস্থিত।"),
    ("জাতিসংঘের সদর দপ্তর কোথায়?", "নিউইয়র্ক", ["জেনেভা", "লন্ডন", "প্যারিস"],
     "জাতিসংঘের সদর দপ্তর যুক্তরাষ্ট্রের নিউইয়র্কে।"),
    ("জাতিসংঘ শান্তিরক্ষা মিশনে বাংলাদেশ প্রথম অংশ নেয় কোন সালে?", "১৯৮৮", ["১৯৭৪", "১৯৯১", "২০০০"],
     "১৯৮৮ সালে ইরান-ইরাক সীমান্তে পর্যবেক্ষক দলে অংশগ্রহণের মাধ্যমে।"),
]


# ----------------------------------------------------------------------------- generators
# Each generator takes (rng) and returns a question dict. "fixed" generators always produce the
# same question, so they naturally repeat across boards and years.

def g_ssc_math(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        k = rng.choice([4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15])
        return mcq(1, f"√{bn(k * k)} এর মান কত?", bn(k), [bn(x) for x in near(rng, k)],
                   f"{bn(k)} × {bn(k)} = {bn(k * k)}, তাই বর্গমূল {bn(k)}।", rng)
    if ch == 2:
        n = rng.randint(2, 6)
        if rng.random() < 0.5:
            return mcq(2, f"{bn(n)} উপাদানবিশিষ্ট কোনো সেটের উপসেট সংখ্যা কত?", bn(2 ** n),
                       [bn(x) for x in (n * 2, 2 ** n - 1, n * n + 1) if x != 2 ** n][:3] or [bn(2 ** n + 1)],
                       f"n উপাদানবিশিষ্ট সেটের উপসেট সংখ্যা 2ⁿ = {bn(2 ** n)}।", rng)
        a, b, x = rng.randint(2, 7), rng.randint(1, 9), rng.randint(1, 6)
        ans = a * x + b
        return mcq(2, f"f(x) = {bn(a)}x + {bn(b)} হলে f({bn(x)}) এর মান কত?", bn(ans),
                   [bn(v) for v in near(rng, ans)], f"f({bn(x)}) = {bn(a)}×{bn(x)} + {bn(b)} = {bn(ans)}।", rng)
    s, p = rng.randint(4, 12), rng.randint(2, 15)
    ans = s * s - 2 * p
    return mcq(3, f"a + b = {bn(s)} এবং ab = {bn(p)} হলে a² + b² এর মান কত?", bn(ans),
               [bn(v) for v in near(rng, ans, positive=False)], f"a² + b² = (a + b)² − 2ab = {bn(s * s)} − {bn(2 * p)} = {bn(ans)}।", rng)


def g_ssc_math_fixed(rng):
    items = [
        mcq(1, "নিচের কোনটি মূলদ সংখ্যা?", "০.৩৩৩...", ["√২", "√৩", "π"], "আবৃত্ত দশমিক ভগ্নাংশ মূলদ সংখ্যা, কারণ ০.৩৩৩... = ১/৩।", rng),
        mcq(2, "A = {১, ২, ৩} এবং B = {২, ৩, ৪} হলে A ∩ B = ?", "{২, ৩}", ["{১, ৪}", "{১, ২, ৩, ৪}", "{৪}"], "উভয় সেটের সাধারণ উপাদান ২ ও ৩।", rng),
        mcq(3, "(a + b)² এর বিস্তৃতি কোনটি?", "a² + 2ab + b²", ["a² + b²", "a² − 2ab + b²", "a² + ab + b²"], "(a + b)² = a² + 2ab + b²।", rng),
    ]
    return rng.choice(items)


def cq_ssc_math(rng):
    k = rng.randint(3, 7)
    sq, cube = k * k - 2, k ** 3 - 3 * k
    fourth = sq * sq - 2
    return cq(3, f"x + 1/x = {bn(k)} একটি বীজগাণিতিক সম্পর্ক।", [
        ("(x − 1/x)² কে x + 1/x এর মাধ্যমে প্রকাশ কর।", "(x − 1/x)² = (x + 1/x)² − ৪।"),
        (f"x² + 1/x² এর মান নির্ণয় কর।", f"x² + 1/x² = (x + 1/x)² − ২ = {bn(k * k)} − ২ = {bn(sq)}।"),
        (f"x³ + 1/x³ এর মান নির্ণয় কর।", f"x³ + 1/x³ = (x + 1/x)³ − ৩(x + 1/x) = {bn(k ** 3)} − {bn(3 * k)} = {bn(cube)}।"),
        (f"দেখাও যে, x⁴ + 1/x⁴ = {bn(fourth)}।", f"x⁴ + 1/x⁴ = (x² + 1/x²)² − ২ = {bn(sq * sq)} − ২ = {bn(fourth)}।"),
    ])


def g_ssc_bgs(rng):
    ch = rng.randint(1, 3)
    pool = {1: LIBERATION_FACTS, 2: CONSTITUTION_FACTS, 3: INTERNATIONAL_FACTS}[ch]
    q, a, d, e = rng.choice(pool)
    return mcq(ch, q, a, d, e, rng)


def cq_ssc_bgs(rng):
    return rng.choice([
        cq(1, "রফিক সাহেব তাঁর ছেলেকে বলেন, ১৯৭১ সালে তিনি একটি বিশেষ অঞ্চলে সেক্টর কমান্ডারের অধীনে গেরিলা যুদ্ধে অংশ নিয়েছিলেন। "
              "সে সময় একটি অস্থায়ী সরকার যুদ্ধ পরিচালনা করেছিল।", [
            ("মুজিবনগর সরকার কবে শপথ গ্রহণ করে?", "১৭ এপ্রিল ১৯৭১।"),
            ("সেক্টর গঠনের প্রয়োজনীয়তা ব্যাখ্যা কর।", "সমন্বিতভাবে যুদ্ধ পরিচালনা ও প্রশাসনিক সুবিধার জন্য দেশকে ১১টি সেক্টরে ভাগ করা হয়।"),
            ("উদ্দীপকের অস্থায়ী সরকারের সাথে মুজিবনগর সরকারের সাদৃশ্য ব্যাখ্যা কর।", "উভয়ই যুদ্ধকালীন সময়ে মুক্তিযুদ্ধ পরিচালনা, কূটনৈতিক সমর্থন আদায় ও প্রশাসন পরিচালনা করেছে।"),
            ("মুক্তিযুদ্ধে গেরিলা যুদ্ধের ভূমিকা মূল্যায়ন কর।", "গেরিলা আক্রমণ শত্রুবাহিনীর মনোবল ভেঙে দেয়, যোগাযোগব্যবস্থা বিচ্ছিন্ন করে এবং চূড়ান্ত বিজয় ত্বরান্বিত করে।"),
        ]),
        cq(2, "একটি দেশের সর্বোচ্চ আইন জনগণের মৌলিক অধিকার নিশ্চিত করে এবং রাষ্ট্রের চারটি মূলনীতি নির্ধারণ করে।", [
            ("বাংলাদেশের সংবিধান কবে কার্যকর হয়?", "১৬ ডিসেম্বর ১৯৭২।"),
            ("সংবিধানকে রাষ্ট্রের সর্বোচ্চ আইন বলা হয় কেন?", "সংবিধানের সাথে অসামঞ্জস্যপূর্ণ যেকোনো আইন বাতিল বলে গণ্য হয়।"),
            ("উদ্দীপকের মূলনীতিগুলো ব্যাখ্যা কর।", "জাতীয়তাবাদ, সমাজতন্ত্র, গণতন্ত্র ও ধর্মনিরপেক্ষতা: রাষ্ট্র পরিচালনার মূল ভিত্তি।"),
            ("মৌলিক অধিকার সংরক্ষণে সংবিধানের ভূমিকা বিশ্লেষণ কর।", "সংবিধান মৌলিক অধিকারের নিশ্চয়তা দেয় এবং লঙ্ঘিত হলে উচ্চ আদালতে প্রতিকার পাওয়া যায়।"),
        ]),
    ])


STORAGE_UNITS = [("কিলোবাইট", "বাইট", 1024), ("মেগাবাইট", "কিলোবাইট", 1024), ("বাইট", "বিট", 8)]


def g_ssc_ict(rng):
    ch = rng.randint(1, 3)
    if ch == 2:
        big, small, factor = rng.choice(STORAGE_UNITS)
        n = rng.randint(2, 8)
        ans = n * factor
        return mcq(2, f"{bn(n)} {big} = কত {small}?", bn(ans), [bn(v) for v in (n * 1000, ans // 2, ans + factor)],
                   f"১ {big} = {bn(factor)} {small}, তাই {bn(n)} × {bn(factor)} = {bn(ans)}।", rng)
    if ch == 3:
        vals = [rng.randint(5, 60) for _ in range(3)]
        fn = rng.choice(["SUM", "MAX", "MIN"])
        ans = {"SUM": sum(vals), "MAX": max(vals), "MIN": min(vals)}[fn]
        cells = ", ".join(f"A{i + 1} = {bn(v)}" for i, v in enumerate(vals))
        distractors = {sum(vals), max(vals), min(vals), round(sum(vals) / 3)} - {ans}
        while len(distractors) < 3:
            distractors.add(ans + rng.randint(1, 9))
        return mcq(3, f"স্প্রেডশিটে {cells} হলে ={fn}(A1:A3) সূত্রের ফলাফল কত?", bn(ans),
                   [bn(v) for v in list(distractors)[:3]], f"{fn} ফাংশনটি A1 থেকে A3 পর্যন্ত মানের উপর কাজ করে ফলাফল দেয় {bn(ans)}।", rng)
    return rng.choice([
        mcq(1, "নিচের কোনটি ই-কমার্সের উদাহরণ?", "অনলাইনে পণ্য কেনাবেচা", ["চিঠি লেখা", "রেডিও শোনা", "হাতে হিসাব রাখা"], "ইন্টারনেটের মাধ্যমে পণ্য বা সেবা কেনাবেচাই ই-কমার্স।", rng),
        mcq(1, "ই-লার্নিং বলতে কী বোঝায়?", "ইলেকট্রনিক মাধ্যমে শিক্ষা গ্রহণ", ["শুধু শ্রেণিকক্ষে পাঠদান", "কাগজের বই পড়া", "হাতে লেখা নোট"], "ইন্টারনেট ও ডিজিটাল ডিভাইস ব্যবহার করে শেখাই ই-লার্নিং।", rng),
    ])


def g_ssc_ict_fixed(rng):
    return rng.choice([
        mcq(2, "কম্পিউটার ভাইরাস থেকে রক্ষা পেতে কোনটি ব্যবহার করা হয়?", "অ্যান্টিভাইরাস সফটওয়্যার", ["ওয়ার্ড প্রসেসর", "স্প্রেডশিট", "মিডিয়া প্লেয়ার"], "অ্যান্টিভাইরাস ক্ষতিকর প্রোগ্রাম শনাক্ত ও অপসারণ করে।", rng),
        mcq(3, "স্প্রেডশিটে সংখ্যার যোগফল নির্ণয়ে কোন ফাংশন ব্যবহৃত হয়?", "SUM", ["AVERAGE", "COUNT", "MAX"], "SUM ফাংশন নির্দিষ্ট সেলগুলোর যোগফল দেয়।", rng),
    ])


def cq_ssc_ict(rng):
    vals = [rng.randint(40, 95) for _ in range(4)]
    total, avg = sum(vals), round(sum(vals) / 4, 2)
    return cq(3, f"রিনা স্প্রেডশিটে চার বিষয়ের নম্বর B2:B5 সেলে লিখল: {', '.join(bn(v) for v in vals)}।", [
        ("সেল অ্যাড্রেস কাকে বলে?", "কলাম ও সারির নাম মিলিয়ে কোনো সেলের পরিচয়কে সেল অ্যাড্রেস বলে, যেমন B2।"),
        ("স্প্রেডশিটে ফাংশন ব্যবহারের সুবিধা ব্যাখ্যা কর।", "ফাংশন ব্যবহারে জটিল হিসাব দ্রুত ও নির্ভুলভাবে করা যায় এবং তথ্য পরিবর্তন হলে ফলাফল নিজে থেকে হালনাগাদ হয়।"),
        ("রিনার মোট নম্বর নির্ণয়ের সূত্র লিখে মান বের কর।", f"=SUM(B2:B5) = {bn(total)}।"),
        ("গড় নম্বর নির্ণয়ে AVERAGE ফাংশন ব্যবহারের যৌক্তিকতা বিশ্লেষণ কর।", f"=AVERAGE(B2:B5) = {fmt(avg)}; এটি যোগ ও ভাগ আলাদাভাবে করার ঝামেলা কমায়।"),
    ])


def g_ssc_physics(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        n = rng.randint(2, 15)
        return mcq(1, f"{bn(n)} কিলোমিটার = কত মিটার?", bn(n * 1000), [bn(n * 100), bn(n * 10), bn(n * 10000)],
                   f"১ কিলোমিটার = ১০০০ মিটার, তাই {bn(n)} কিমি = {bn(n * 1000)} মিটার।", rng)
    if ch == 2:
        a, t = rng.randint(2, 6), rng.randint(3, 10)
        if rng.random() < 0.5:
            v = a * t
            return mcq(2, f"স্থির অবস্থান থেকে {bn(a)} m/s² সুষম ত্বরণে চলা একটি বস্তুর {bn(t)} সেকেন্ড পর বেগ কত?",
                       f"{bn(v)} m/s", [f"{bn(x)} m/s" for x in near(rng, v)], f"v = u + at = ০ + {bn(a)}×{bn(t)} = {bn(v)} m/s।", rng)
        s = a * t * t / 2
        return mcq(2, f"স্থির অবস্থান থেকে {bn(a)} m/s² ত্বরণে {bn(t)} সেকেন্ডে অতিক্রান্ত দূরত্ব কত?",
                   f"{fmt(s)} m", [f"{fmt(x)} m" for x in (a * t * t, a * t, s + a)], f"s = ½at² = ½×{bn(a)}×{bn(t * t)} = {fmt(s)} m।", rng)
    m, a = rng.randint(2, 20), rng.randint(2, 9)
    f = m * a
    return mcq(3, f"{bn(m)} kg ভরের একটি বস্তুতে {bn(a)} m/s² ত্বরণ সৃষ্টি করতে কত বল প্রয়োজন?",
               f"{bn(f)} N", [f"{bn(x)} N" for x in near(rng, f)], f"F = ma = {bn(m)} × {bn(a)} = {bn(f)} N।", rng)


def g_ssc_physics_fixed(rng):
    return rng.choice([
        mcq(1, "নিচের কোনটি মৌলিক রাশি?", "ভর", ["বেগ", "বল", "ক্ষেত্রফল"], "দৈর্ঘ্য, ভর, সময়, তাপমাত্রা, তড়িৎপ্রবাহ, দীপন তীব্রতা ও পদার্থের পরিমাণ মৌলিক রাশি।", rng),
        mcq(2, "বেগের মাত্রা কোনটি?", "LT⁻¹", ["LT⁻²", "MLT⁻²", "ML²T⁻²"], "বেগ = সরণ/সময়, তাই মাত্রা LT⁻¹।", rng),
        mcq(3, "বলের এসআই একক কোনটি?", "নিউটন", ["জুল", "ওয়াট", "প্যাসকেল"], "১ নিউটন = ১ kg·m/s²।", rng),
    ])


def cq_ssc_physics(rng):
    u, a, t = rng.randint(0, 10), rng.randint(2, 5), rng.randint(4, 10)
    v = u + a * t
    s = u * t + a * t * t / 2
    return cq(2, f"একটি গাড়ি {bn(u)} m/s আদিবেগে চলা শুরু করে {bn(a)} m/s² সুষম ত্বরণে {bn(t)} সেকেন্ড চলল।", [
        ("ত্বরণ কাকে বলে?", "সময়ের সাথে বস্তুর বেগ বৃদ্ধির হারকে ত্বরণ বলে।"),
        ("সুষম ত্বরণ বলতে কী বোঝায়? ব্যাখ্যা কর।", "বস্তুর বেগ যদি সমান সময়ে সমান পরিমাণে বাড়ে তবে সেই ত্বরণকে সুষম ত্বরণ বলে।"),
        ("গাড়িটির শেষবেগ নির্ণয় কর।", f"v = u + at = {bn(u)} + {bn(a)}×{bn(t)} = {bn(v)} m/s।"),
        ("গাড়িটি ওই সময়ে কত দূরত্ব অতিক্রম করবে? গাণিতিকভাবে বিশ্লেষণ কর।", f"s = ut + ½at² = {bn(u * t)} + {fmt(a * t * t / 2)} = {fmt(s)} m।"),
    ])


def g_ssc_chemistry(rng):
    ch = rng.randint(1, 3)
    z, sym, name, mass = rng.choice(ELEMENTS[:20])
    if ch == 1:
        others = [e[1] for e in ELEMENTS if e[1] != sym]
        return mcq(1, f"{name} এর প্রতীক কোনটি?", sym, rng.sample(others, 3), f"{name} এর রাসায়নিক প্রতীক {sym}।", rng)
    if ch == 2:
        others = [e[2] for e in ELEMENTS if e[0] != z]
        return mcq(2, f"কোন মৌলের পারমাণবিক সংখ্যা {bn(z)}?", name, rng.sample(others, 3), f"{name} ({sym}) এর পারমাণবিক সংখ্যা {bn(z)}।", rng)
    neutrons = mass - z
    if rng.random() < 0.5:
        return mcq(3, f"{name} পরমাণুর ভরসংখ্যা {bn(mass)} এবং পারমাণবিক সংখ্যা {bn(z)} হলে নিউট্রন সংখ্যা কত?", bn(neutrons),
                   [bn(x) for x in near(rng, neutrons, positive=False) if x >= 0] or [bn(neutrons + 1), bn(neutrons + 2), bn(neutrons + 3)],
                   f"নিউট্রন সংখ্যা = ভরসংখ্যা − পারমাণবিক সংখ্যা = {bn(mass)} − {bn(z)} = {bn(neutrons)}।", rng)
    if z < 5:
        z, sym, name, mass = rng.choice(ELEMENTS[4:20])
    shells = shell_counts(z)
    correct = ", ".join(bn(s) for s in shells)
    wrong = set()
    for _ in range(100):
        if len(wrong) == 3:
            break
        alt = list(shells)
        i = rng.randrange(len(alt))
        alt[i] = max(1, alt[i] + rng.choice([-1, 1]))
        cand = ", ".join(bn(s) for s in alt)
        if cand != correct:
            wrong.add(cand)
    return mcq(3, f"{name} ({sym}, Z = {bn(z)}) এর শক্তিস্তরে ইলেকট্রন বিন্যাস কোনটি?", correct, list(wrong),
               f"২, ৮, ৮ নিয়মে সাজালে {name} এর বিন্যাস {correct}।", rng)


def cq_ssc_chemistry(rng):
    z, sym, name, mass = rng.choice(ELEMENTS[2:20])
    shells = ", ".join(bn(s) for s in shell_counts(z))
    return cq(3, f"X একটি মৌল যার পারমাণবিক সংখ্যা {bn(z)} এবং ভরসংখ্যা {bn(mass)}।", [
        ("পারমাণবিক সংখ্যা কাকে বলে?", "কোনো মৌলের পরমাণুর নিউক্লিয়াসে উপস্থিত প্রোটন সংখ্যাকে পারমাণবিক সংখ্যা বলে।"),
        ("আইসোটোপ বলতে কী বোঝায়? ব্যাখ্যা কর।", "একই মৌলের যেসব পরমাণুর প্রোটন সংখ্যা সমান কিন্তু ভরসংখ্যা ভিন্ন, তাদের আইসোটোপ বলে।"),
        ("X মৌলের ইলেকট্রন বিন্যাস লেখ।", f"X হলো {name} ({sym}); শক্তিস্তরে বিন্যাস {shells}।"),
        ("X এর নিউক্লিয়াসের গঠন বিশ্লেষণ কর।", f"প্রোটন {bn(z)}টি এবং নিউট্রন {bn(mass)} − {bn(z)} = {bn(mass - z)}টি।"),
    ])


def g_ssc_biology(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        common, sci = rng.choice(SCIENTIFIC_NAMES)
        others = [s for _, s in SCIENTIFIC_NAMES if s != sci]
        return mcq(1, f"{common} এর বৈজ্ঞানিক নাম কোনটি?", sci, rng.sample(others, 3), f"{common} এর বৈজ্ঞানিক নাম {sci}।", rng)
    if ch == 2:
        organelle, job = rng.choice(ORGANELLES)
        others = [o for o, _ in ORGANELLES if o != organelle]
        return mcq(2, f"কোন অঙ্গাণুর প্রধান কাজ {job}?", organelle, rng.sample(others, 3), f"{organelle} এর প্রধান কাজ {job}।", rng)
    org, n2 = rng.choice(CHROMOSOMES)
    return mcq(3, f"{org} এর দেহকোষে {bn(n2)}টি ক্রোমোজোম থাকলে মাইটোসিসের পর প্রতিটি অপত্য কোষে কয়টি থাকবে?", f"{bn(n2)}টি",
               [f"{bn(n2 // 2)}টি", f"{bn(n2 * 2)}টি", f"{bn(n2 + 2)}টি"], "মাইটোসিসে অপত্য কোষে মাতৃকোষের সমান সংখ্যক ক্রোমোজোম থাকে।", rng)


def g_ssc_biology_fixed(rng):
    return rng.choice([
        mcq(2, "কোষের শক্তিঘর বলা হয় কোনটিকে?", "মাইটোকন্ড্রিয়া", ["রাইবোসোম", "লাইসোসোম", "গলগি বস্তু"], "মাইটোকন্ড্রিয়াতে শ্বসনের মাধ্যমে শক্তি উৎপন্ন হয়।", rng),
        mcq(3, "মিয়োসিস কোষ বিভাজনে কয়টি অপত্য কোষ সৃষ্টি হয়?", "৪টি", ["২টি", "৮টি", "১টি"], "মিয়োসিসে একটি কোষ বিভাজিত হয়ে চারটি অপত্য কোষ তৈরি হয়।", rng),
    ])


def cq_ssc_biology(rng):
    org, n2 = rng.choice(CHROMOSOMES)
    return cq(3, f"{org} এর একটি দেহকোষে {bn(n2)}টি ক্রোমোজোম আছে। কোষটি একবার মাইটোসিস এবং একটি জনন মাতৃকোষ মিয়োসিস প্রক্রিয়ায় বিভাজিত হলো।", [
        ("ক্রোমোজোম কাকে বলে?", "নিউক্লিয়াসে অবস্থিত বংশগতির বাহক সূত্রাকার বস্তুকে ক্রোমোজোম বলে।"),
        ("মাইটোসিসকে সমীকরণিক বিভাজন বলা হয় কেন?", "কারণ এতে অপত্য কোষে মাতৃকোষের সমান সংখ্যক ক্রোমোজোম থাকে।"),
        ("মিয়োসিসের পর প্রতিটি কোষে ক্রোমোজোম সংখ্যা নির্ণয় কর।", f"মিয়োসিসে সংখ্যা অর্ধেক হয়: {bn(n2)} ÷ ২ = {bn(n2 // 2)}টি।"),
        ("জীবের বংশধারা রক্ষায় দুই প্রকার বিভাজনের তুলনামূলক ভূমিকা বিশ্লেষণ কর।", "মাইটোসিস দেহের বৃদ্ধি ও ক্ষয়পূরণ করে, আর মিয়োসিস জনন কোষে ক্রোমোজোম সংখ্যা অর্ধেক রেখে প্রজন্মান্তরে সংখ্যা স্থির রাখে।"),
    ])


def g_ssc_accounting(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        liab, equity = rng.randint(2, 40) * 1000, rng.randint(5, 80) * 1000
        assets = liab + equity
        return mcq(1, f"কোনো প্রতিষ্ঠানের দায় {bn(liab)} টাকা এবং মালিকানাস্বত্ব {bn(equity)} টাকা হলে মোট সম্পদ কত?", f"{bn(assets)} টাকা",
                   [f"{bn(x)} টাকা" for x in (abs(equity - liab), equity, assets + 1000)], "হিসাব সমীকরণ: সম্পদ = দায় + মালিকানাস্বত্ব।", rng)
    if ch == 2:
        return rng.choice([
            mcq(2, "নিচের কোনটি লেনদেন?", "নগদে আসবাবপত্র ক্রয়", ["কর্মচারী নিয়োগের সিদ্ধান্ত", "পণ্যের অর্ডার গ্রহণ", "ব্যবসায়ের পরিকল্পনা"], "যে ঘটনায় আর্থিক অবস্থার পরিবর্তন ঘটে এবং অর্থের অঙ্কে পরিমাপযোগ্য, তা-ই লেনদেন।", rng),
            mcq(2, "মালিক কর্তৃক ব্যক্তিগত প্রয়োজনে পণ্য উত্তোলনকে কী বলে?", "উত্তোলন", ["আয়", "মূলধন", "দায়"], "ব্যবসায় থেকে মালিকের ব্যক্তিগত ব্যবহারের জন্য অর্থ বা পণ্য নেওয়াই উত্তোলন।", rng),
        ])
    amount = rng.randint(2, 50) * 1000
    item, debit, credit = rng.choice([("নগদে পণ্য বিক্রয়", "নগদান হিসাব", "বিক্রয় হিসাব"),
                                      ("নগদে পণ্য ক্রয়", "ক্রয় হিসাব", "নগদান হিসাব"),
                                      ("বেতন প্রদান", "বেতন হিসাব", "নগদান হিসাব")])
    return mcq(3, f"{item} {bn(amount)} টাকা: এই লেনদেনে কোন হিসাবটি ডেবিট হবে?", debit,
               [credit] + rng.sample([x for x in ["মূলধন হিসাব", "উত্তোলন হিসাব", "ভাড়া হিসাব", "পাওনাদার হিসাব"] if x not in (debit, credit)], 2),
               f"দুতরফা দাখিলা অনুযায়ী {debit} ডেবিট এবং {credit} ক্রেডিট হবে।", rng)


def cq_ssc_accounting(rng):
    cap, purchase, sale = rng.randint(5, 20) * 10000, rng.randint(2, 8) * 5000, rng.randint(3, 10) * 5000
    return cq(3, f"জনাব করিম {bn(cap)} টাকা নিয়ে ব্যবসায় শুরু করলেন। তিনি নগদে {bn(purchase)} টাকার পণ্য ক্রয় এবং {bn(sale)} টাকার পণ্য নগদে বিক্রয় করলেন।", [
        ("দুতরফা দাখিলা পদ্ধতি কী?", "প্রতিটি লেনদেনের দুটি পক্ষকে একই অঙ্কে ডেবিট ও ক্রেডিট করে লিপিবদ্ধ করার পদ্ধতি।"),
        ("প্রতিটি লেনদেনের দুটি পক্ষ থাকে কেন?", "কারণ প্রতিটি লেনদেনে একটি পক্ষ সুবিধা গ্রহণ করে এবং অপর পক্ষ সুবিধা প্রদান করে।"),
        ("উদ্দীপকের লেনদেনগুলোর জাবেদা দাখিলা দাও।", f"নগদান হি. ডে. {bn(cap)} / মূলধন হি. ক্রে.; ক্রয় হি. ডে. {bn(purchase)} / নগদান হি. ক্রে.; নগদান হি. ডে. {bn(sale)} / বিক্রয় হি. ক্রে.।"),
        ("লেনদেন শেষে নগদ উদ্বৃত্ত নির্ণয় করে বিশ্লেষণ কর।", f"নগদ উদ্বৃত্ত = {bn(cap)} − {bn(purchase)} + {bn(sale)} = {bn(cap - purchase + sale)} টাকা।"),
    ])


def to_base(n, base):
    digits = "0123456789ABCDEF"
    out = ""
    while n:
        out = digits[n % base] + out
        n //= base
    return out or "0"


def g_hsc_ict(rng):
    ch = rng.randint(1, 3)
    if ch == 3:
        n = rng.randint(9, 250)
        kind = rng.choice([(2, "বাইনারি"), (8, "অক্টাল"), (16, "হেক্সাডেসিমেল")])
        ans = to_base(n, kind[0])
        wrong = {to_base(n + d, kind[0]) for d in (1, 2, -1, 3, 4) if n + d > 0} - {ans}
        return mcq(3, f"দশমিক {n} এর সমতুল্য {kind[1]} সংখ্যা কোনটি?", f"({ans})₍{kind[0]}₎",
                   [f"({w})₍{kind[0]}₎" for w in list(wrong)[:3]], f"{n} কে বারবার {kind[0]} দিয়ে ভাগ করে ভাগশেষগুলো উল্টো ক্রমে সাজালে {ans} পাওয়া যায়।", rng)
    if ch == 2:
        return rng.choice([
            mcq(2, "সর্বোচ্চ ব্যান্ডউইথ প্রদান করে কোন মাধ্যম?", "অপটিক্যাল ফাইবার", ["টুইস্টেড পেয়ার ক্যাবল", "কো-এক্সিয়াল ক্যাবল", "ব্লুটুথ"], "আলোর মাধ্যমে ডেটা পাঠানোয় অপটিক্যাল ফাইবারের ব্যান্ডউইথ সবচেয়ে বেশি।", rng),
            mcq(2, "যে নেটওয়ার্কে সবগুলো কম্পিউটার একটি কেন্দ্রীয় হাবের সাথে যুক্ত থাকে তাকে কী বলে?", "স্টার টপোলজি", ["বাস টপোলজি", "রিং টপোলজি", "মেশ টপোলজি"], "স্টার টপোলজিতে প্রতিটি নোড কেন্দ্রীয় হাব বা সুইচের সাথে যুক্ত থাকে।", rng),
            mcq(2, "ব্লুটুথ কোন ধরনের নেটওয়ার্কের উদাহরণ?", "PAN", ["LAN", "MAN", "WAN"], "স্বল্প দূরত্বে ব্যক্তিগত ডিভাইস সংযুক্ত করে বলে ব্লুটুথ PAN।", rng),
        ])
    return rng.choice([
        mcq(1, "ভার্চুয়াল রিয়েলিটিতে ব্যবহৃত হয় কোনটি?", "হেড মাউন্টেড ডিসপ্লে", ["প্রিন্টার", "স্ক্যানার", "প্লটার"], "কৃত্রিম ত্রিমাত্রিক পরিবেশ অনুভবে হেড মাউন্টেড ডিসপ্লে ব্যবহৃত হয়।", rng),
        mcq(1, "রোবটিক্সে কোন প্রযুক্তি সবচেয়ে বেশি ব্যবহৃত হয়?", "কৃত্রিম বুদ্ধিমত্তা", ["ক্লাউড স্টোরেজ", "ই-মেইল", "ওয়ার্ড প্রসেসিং"], "রোবটকে সিদ্ধান্ত নিতে সক্ষম করতে কৃত্রিম বুদ্ধিমত্তা ব্যবহৃত হয়।", rng),
    ])


def cq_hsc_ict(rng):
    a, b = rng.randint(20, 120), rng.randint(20, 120)
    return cq(3, f"দুটি দশমিক সংখ্যা P = {a} এবং Q = {b}।", [
        ("বিট কী?", "বাইনারি সংখ্যা পদ্ধতির ক্ষুদ্রতম একক (০ বা ১)।"),
        ("কম্পিউটার কেন বাইনারি পদ্ধতি ব্যবহার করে? ব্যাখ্যা কর।", "ইলেকট্রনিক সার্কিট দুটি অবস্থা (অন/অফ) সহজে নির্ভুলভাবে প্রকাশ করতে পারে, তাই বাইনারি ব্যবহৃত হয়।"),
        ("P কে বাইনারি ও হেক্সাডেসিমেলে রূপান্তর কর।", f"({a})₁₀ = ({to_base(a, 2)})₂ = ({to_base(a, 16)})₁₆।"),
        ("P + Q এর যোগফল বাইনারিতে নির্ণয় করে যাচাই কর।", f"({to_base(a, 2)})₂ + ({to_base(b, 2)})₂ = ({to_base(a + b, 2)})₂ = ({a + b})₁₀।"),
    ])


def g_hsc_physics(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        return rng.choice([
            mcq(1, "বলের মাত্রা কোনটি?", "MLT⁻²", ["MLT⁻¹", "ML²T⁻²", "ML⁻¹T⁻²"], "বল = ভর × ত্বরণ, তাই মাত্রা MLT⁻²।", rng),
            mcq(1, "কাজের মাত্রা কোনটি?", "ML²T⁻²", ["MLT⁻²", "ML²T⁻³", "ML⁻¹T⁻²"], "কাজ = বল × সরণ, তাই মাত্রা ML²T⁻²।", rng),
            mcq(1, "চাপের মাত্রা কোনটি?", "ML⁻¹T⁻²", ["MLT⁻²", "ML²T⁻²", "ML⁻²T⁻¹"], "চাপ = বল/ক্ষেত্রফল, তাই মাত্রা ML⁻¹T⁻²।", rng),
        ])
    if ch == 2:
        x, y = rng.choice([(3, 4), (6, 8), (5, 12), (8, 15), (7, 24), (9, 12)])
        mag = int(math.hypot(x, y))
        return mcq(2, f"A⃗ = {bn(x)}î + {bn(y)}ĵ হলে |A⃗| এর মান কত?", bn(mag), [bn(v) for v in (x + y, abs(y - x), mag + 1)],
                   f"|A⃗| = √({bn(x)}² + {bn(y)}²) = √{bn(x * x + y * y)} = {bn(mag)}।", rng)
    u = rng.choice([10, 20, 30, 40])
    theta = rng.choice([30, 45, 60, 90])
    g = 9.8
    h = round((u * math.sin(math.radians(theta))) ** 2 / (2 * g), 2)
    return mcq(3, f"একটি প্রাসকে {bn(u)} m/s বেগে অনুভূমিকের সাথে {bn(theta)}° কোণে নিক্ষেপ করলে সর্বোচ্চ উচ্চতা কত? (g = ৯.৮ m/s²)",
               f"{fmt(h)} m", [f"{fmt(round(h * f, 2))} m" for f in (2, 0.5, 1.5)], f"H = u²sin²θ / 2g = {fmt(h)} m।", rng)


def cq_hsc_physics(rng):
    u, theta = rng.choice([20, 30, 40]), rng.choice([30, 45, 60])
    g = 9.8
    s = math.sin(math.radians(theta))
    t = round(2 * u * s / g, 2)
    h = round((u * s) ** 2 / (2 * g), 2)
    r = round(u * u * math.sin(math.radians(2 * theta)) / g, 2)
    return cq(3, f"একটি বলকে {bn(u)} m/s বেগে অনুভূমিকের সাথে {bn(theta)}° কোণে নিক্ষেপ করা হলো। (g = ৯.৮ m/s²)", [
        ("প্রাস কাকে বলে?", "কোনো বস্তুকে অনুভূমিকের সাথে তির্যকভাবে নিক্ষেপ করলে তা যে পথে চলে, তাকে প্রাস বলে।"),
        ("সর্বোচ্চ উচ্চতায় প্রাসের বেগ শূন্য হয় না কেন? ব্যাখ্যা কর।", "সর্বোচ্চ বিন্দুতে বেগের উল্লম্ব উপাংশ শূন্য হলেও অনুভূমিক উপাংশ u cosθ বিদ্যমান থাকে।"),
        ("বলটির বিচরণকাল ও সর্বোচ্চ উচ্চতা নির্ণয় কর।", f"T = 2u sinθ / g = {fmt(t)} s; H = u²sin²θ / 2g = {fmt(h)} m।"),
        ("নিক্ষেপ কোণ ৪৫° হলে অনুভূমিক পাল্লা সর্বোচ্চ হবে কি না, গাণিতিকভাবে বিশ্লেষণ কর।", f"R = u²sin2θ / g; θ = ৪৫° হলে sin২θ = ১ সর্বোচ্চ। বর্তমান পাল্লা {fmt(r)} m।"),
    ])


def g_hsc_chemistry(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        return rng.choice([
            mcq(1, "ল্যাবরেটরিতে দাহ্য পদার্থ রাখার জন্য কোন সংকেত ব্যবহৃত হয়?", "আগুনের শিখার চিহ্ন", ["মাথার খুলির চিহ্ন", "বিস্ফোরণের চিহ্ন", "গাছ ও মাছের চিহ্ন"], "দাহ্য পদার্থ শিখার চিহ্ন দিয়ে চিহ্নিত করা হয়।", rng),
            mcq(1, "অ্যাসিড পাতলা করার সঠিক নিয়ম কোনটি?", "পানিতে ধীরে ধীরে অ্যাসিড যোগ করা", ["অ্যাসিডে পানি যোগ করা", "দুটি একসাথে ঢালা", "অ্যাসিড গরম করে পানি দেওয়া"], "অ্যাসিডে পানি দিলে প্রচুর তাপ উৎপন্ন হয়ে ছিটকে আসতে পারে।", rng),
        ])
    z, sym, name, _ = rng.choice([e for e in ELEMENTS if e[0] >= 3])
    if ch == 2:
        correct = electron_config(z)
        wrong = {electron_config(o[0]) for o in rng.sample([e for e in ELEMENTS if e[0] != z and e[0] >= 3], 3)}
        return mcq(2, f"{name} ({sym}, Z = {bn(z)}) এর সঠিক ইলেকট্রন বিন্যাস কোনটি?", correct, list(wrong)[:3],
                   "আউফবাউ নীতি অনুসারে নিম্ন শক্তির অরবিটাল আগে পূর্ণ হয়" + ("; Cr ও Cu ব্যতিক্রম।" if z in CONFIG_EXCEPTIONS else "।"), rng)
    period = 1 if z <= 2 else 2 if z <= 10 else 3 if z <= 18 else 4
    return mcq(3, f"{name} ({sym}, Z = {bn(z)}) পর্যায় সারণির কোন পর্যায়ে অবস্থিত?", f"{bn(period)}য় পর্যায়" if period in (2, 3) else f"{bn(period)}র্থ পর্যায়" if period == 4 else "১ম পর্যায়",
               [p for p in ["১ম পর্যায়", "২য় পর্যায়", "৩য় পর্যায়", "৪র্থ পর্যায়"] if not p.startswith(bn(period))][:3],
               f"সর্বশেষ শক্তিস্তরের ক্রমিক সংখ্যাই পর্যায় নির্দেশ করে; {name} এর বিন্যাস {electron_config(z)}।", rng)


def cq_hsc_chemistry(rng):
    z, sym, name, _ = rng.choice([e for e in ELEMENTS if 11 <= e[0] <= 30])
    return cq(2, f"একটি মৌল M এর পারমাণবিক সংখ্যা {bn(z)}।", [
        ("আউফবাউ নীতি কী?", "ইলেকট্রন প্রথমে নিম্ন শক্তির অরবিটালে এবং পরে ক্রমান্বয়ে উচ্চ শক্তির অরবিটালে প্রবেশ করে।"),
        ("৪s অরবিটাল ৩d এর আগে পূর্ণ হয় কেন? ব্যাখ্যা কর।", "(n + l) নিয়মে ৪s এর মান ৪ এবং ৩d এর মান ৫; কম মানের অরবিটাল আগে পূর্ণ হয়।"),
        ("M এর ইলেকট্রন বিন্যাস লেখ।", f"M হলো {name} ({sym}): {electron_config(z)}।"),
        ("M এর বিন্যাসে কোনো ব্যতিক্রম আছে কি না বিশ্লেষণ কর।",
         "অর্ধপূর্ণ বা পূর্ণ d অরবিটাল বেশি সুস্থিত বলে বিন্যাসে ব্যতিক্রম দেখা যায়।" if z in CONFIG_EXCEPTIONS else "এখানে স্বাভাবিক আউফবাউ ক্রম অনুসৃত হয়েছে, তাই কোনো ব্যতিক্রম নেই।"),
    ])


def g_hsc_biology(rng):
    ch = rng.randint(1, 3)
    if ch == 2:
        org, n2 = rng.choice(CHROMOSOMES)
        return mcq(2, f"{org} এর দেহকোষে ২n = {bn(n2)} হলে মিয়োসিস শেষে প্রতিটি কোষে ক্রোমোজোম সংখ্যা কত?", f"{bn(n2 // 2)}টি",
                   [f"{bn(n2)}টি", f"{bn(n2 * 2)}টি", f"{bn(n2 // 2 + 1)}টি"], "মিয়োসিসে ক্রোমোজোম সংখ্যা অর্ধেক (n) হয়ে যায়।", rng)
    if ch == 1:
        organelle, job = rng.choice(ORGANELLES)
        others = [o for o, _ in ORGANELLES if o != organelle]
        return mcq(1, f"{job}: এটি কোন অঙ্গাণুর কাজ?", organelle, rng.sample(others, 3), f"{organelle} এর প্রধান কাজ {job}।", rng)
    return rng.choice([
        mcq(3, "এনজাইম মূলত কোন ধরনের পদার্থ?", "প্রোটিন", ["লিপিড", "কার্বোহাইড্রেট", "নিউক্লিক অ্যাসিড"], "অধিকাংশ এনজাইম প্রোটিনধর্মী জৈব অনুঘটক।", rng),
        mcq(3, "DNA এর গঠনগত একক কোনটি?", "নিউক্লিওটাইড", ["অ্যামাইনো অ্যাসিড", "ফ্যাটি অ্যাসিড", "গ্লুকোজ"], "নিউক্লিওটাইড (শর্করা, ফসফেট ও নাইট্রোজেন বেস) DNA এর একক।", rng),
        mcq(3, "সেলুলোজ কোন ধরনের কার্বোহাইড্রেট?", "পলিস্যাকারাইড", ["মনোস্যাকারাইড", "ডাইস্যাকারাইড", "অলিগোস্যাকারাইড"], "সেলুলোজ বহু গ্লুকোজ অণুর সমন্বয়ে গঠিত পলিস্যাকারাইড।", rng),
    ])


def cq_hsc_biology(rng):
    if rng.random() < 0.7:
        org, n2 = rng.choice(CHROMOSOMES)
        dna = rng.choice([4, 6, 8, 10])
        return cq(2, f"{org} এর একটি জনন মাতৃকোষে ২n = {bn(n2)}টি ক্রোমোজোম এবং G₁ দশায় DNA এর পরিমাণ {bn(dna)} একক।", [
            ("সেন্ট্রোমিয়ার কী?", "ক্রোমোজোমের যে স্থানে দুটি ক্রোমাটিড যুক্ত থাকে তাকে সেন্ট্রোমিয়ার বলে।"),
            ("ইন্টারফেজকে প্রস্তুতি দশা বলা হয় কেন?", "এ দশায় DNA অনুলিপন ও প্রয়োজনীয় প্রোটিন সংশ্লেষণের মাধ্যমে কোষ বিভাজনের প্রস্তুতি নেয়।"),
            ("মিয়োসিস-১ শেষে প্রতিটি কোষে ক্রোমোজোম সংখ্যা ও DNA এর পরিমাণ নির্ণয় কর।", f"ক্রোমোজোম {bn(n2 // 2)}টি এবং DNA {bn(dna)} একক (S দশায় দ্বিগুণ {bn(dna * 2)} হয়ে অর্ধেক হয়)।"),
            ("মিয়োসিস-২ শেষে DNA এর পরিমাণ কত হবে বিশ্লেষণ কর।", f"মিয়োসিস-২ এ ক্রোমাটিড পৃথক হয়, তাই DNA এর পরিমাণ {fmt(dna / 2)} একক এবং ক্রোমোজোম {bn(n2 // 2)}টি থাকে।"),
        ])
    return rng.choice([
        cq(1, "একটি কোষে দ্বিস্তরী আবরণে ঘেরা একটি অঙ্গাণু রয়েছে যার অন্তঃস্তর ভাঁজ হয়ে ক্রিস্টি গঠন করে।", [
            ("ক্রিস্টি কী?", "মাইটোকন্ড্রিয়ার অন্তঃস্তরের আঙুলের মতো ভাঁজকে ক্রিস্টি বলে।"),
            ("উদ্দীপকের অঙ্গাণুকে শক্তিঘর বলা হয় কেন?", "এখানে কোষীয় শ্বসনের মাধ্যমে ATP আকারে শক্তি উৎপন্ন হয়।"),
            ("অঙ্গাণুটির গঠন চিত্রসহ ব্যাখ্যা কর।", "বহিঃস্তর, অন্তঃস্তর, ক্রিস্টি, ম্যাট্রিক্স ও অক্সিসোম নিয়ে মাইটোকন্ড্রিয়া গঠিত।"),
            ("কোষের জীবনে অঙ্গাণুটির গুরুত্ব বিশ্লেষণ কর।", "শক্তি উৎপাদন ছাড়াও এটি নিজস্ব DNA বহন করে, প্রোটিন সংশ্লেষণে অংশ নেয় এবং কোষের বিপাক নিয়ন্ত্রণে সাহায্য করে।"),
        ]),
        cq(2, "একটি জনন মাতৃকোষ বিভাজিত হয়ে চারটি অপত্য কোষ সৃষ্টি করল যাদের ক্রোমোজোম সংখ্যা মাতৃকোষের অর্ধেক।", [
            ("ক্রসিং ওভার কী?", "মিয়োসিসে সমসংস্থ ক্রোমোজোমের নন-সিস্টার ক্রোমাটিডের মধ্যে অংশ বিনিময়কে ক্রসিং ওভার বলে।"),
            ("উদ্দীপকের বিভাজনকে হ্রাসমূলক বিভাজন বলা হয় কেন?", "কারণ এতে ক্রোমোজোম সংখ্যা অর্ধেক হয়ে যায়।"),
            ("বিভাজনটির প্রোফেজ-১ এর উপপর্যায়গুলো ব্যাখ্যা কর।", "লেপ্টোটিন, জাইগোটিন, প্যাকাইটিন, ডিপ্লোটিন ও ডায়াকাইনেসিস।"),
            ("জীবজগতে বৈচিত্র্য সৃষ্টিতে বিভাজনটির ভূমিকা বিশ্লেষণ কর।", "ক্রসিং ওভার ও স্বাধীন বিন্যাসের ফলে নতুন জিন সমন্বয় তৈরি হয়, যা বৈচিত্র্যের উৎস।"),
        ]),
    ])


def det2(m):
    return m[0][0] * m[1][1] - m[0][1] * m[1][0]


def g_hsc_highermath(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        m = [[rng.randint(-5, 9) for _ in range(2)] for _ in range(2)]
        d = det2(m)
        return mcq(1, f"|{bn(m[0][0])}  {bn(m[0][1])}; {bn(m[1][0])}  {bn(m[1][1])}| নির্ণায়কের মান কত?", bn(d),
                   [bn(v) for v in near(rng, d, positive=False)], f"মান = {bn(m[0][0])}×{bn(m[1][1])} − {bn(m[0][1])}×{bn(m[1][0])} = {bn(d)}।", rng)
    if ch == 2:
        a = [rng.randint(-4, 6) for _ in range(3)]
        b = [rng.randint(-4, 6) for _ in range(3)]
        dot = sum(x * y for x, y in zip(a, b))
        vec = lambda v: f"{bn(v[0])}î + {bn(v[1])}ĵ + {bn(v[2])}k̂".replace("+ -", "− ")
        return mcq(2, f"A⃗ = {vec(a)} এবং B⃗ = {vec(b)} হলে A⃗·B⃗ = ?", bn(dot), [bn(v) for v in near(rng, dot, positive=False)],
                   "A⃗·B⃗ = a₁b₁ + a₂b₂ + a₃b₃।", rng)
    x1, y1 = rng.randint(-5, 5), rng.randint(-5, 5)
    dx, dy = rng.choice([1, 2, 3, 4]), rng.randint(-6, 6)
    x2, y2 = x1 + dx, y1 + dy
    from fractions import Fraction
    slope = Fraction(dy, dx)
    s = bn(slope.numerator) if slope.denominator == 1 else f"{bn(slope.numerator)}/{bn(slope.denominator)}"
    wrong = {Fraction(dx, dy) if dy else Fraction(dx + 1), -slope if slope else Fraction(1), slope + 1} - {slope}
    show = lambda f: bn(f.numerator) if f.denominator == 1 else f"{bn(f.numerator)}/{bn(f.denominator)}"
    wrong_list = [show(w) for w in wrong]
    while len(wrong_list) < 3:
        wrong_list.append(show(slope + len(wrong_list) + 2))
    raw = f"{bn(dy)}/{bn(dx)}"
    return mcq(3, f"({bn(x1)}, {bn(y1)}) ও ({bn(x2)}, {bn(y2)}) বিন্দুগামী রেখার ঢাল কত?", s, wrong_list[:3],
               f"ঢাল m = (y₂ − y₁)/(x₂ − x₁) = {raw}" + ("" if raw == s else f" = {s}") + "।", rng)


def cq_hsc_highermath(rng):
    m = [[rng.randint(1, 6) for _ in range(2)] for _ in range(2)]
    d = det2(m)
    if d == 0:
        m[0][0] += 1
        d = det2(m)
    return cq(1, f"A = [{bn(m[0][0])} {bn(m[0][1])}; {bn(m[1][0])} {bn(m[1][1])}] একটি বর্গ ম্যাট্রিক্স।", [
        ("ব্যতিক্রমী ম্যাট্রিক্স কাকে বলে?", "যে বর্গ ম্যাট্রিক্সের নির্ণায়কের মান শূন্য, তাকে ব্যতিক্রমী ম্যাট্রিক্স বলে।"),
        ("A এর বিপরীত ম্যাট্রিক্স থাকবে কি না ব্যাখ্যা কর।", f"|A| = {bn(d)} ≠ ০, তাই A অব্যতিক্রমী এবং এর বিপরীত ম্যাট্রিক্স আছে।"),
        ("A⁻¹ নির্ণয় কর।", f"A⁻¹ = (১/{bn(d)}) × [{bn(m[1][1])} {bn(-m[0][1])}; {bn(-m[1][0])} {bn(m[0][0])}]।"),
        ("AA⁻¹ = I যাচাই কর।", "A ও A⁻¹ গুণ করলে কর্ণের ভুক্তি ১ এবং অন্যান্য ভুক্তি ০ পাওয়া যায়, অর্থাৎ একক ম্যাট্রিক্স I।"),
    ])


def g_hsc_accounting(rng):
    ch = rng.randint(1, 3)
    if ch == 3:
        cash_book = rng.randint(20, 90) * 1000
        uncredited = rng.randint(1, 10) * 1000
        unpresented = rng.randint(1, 10) * 1000
        passbook = cash_book - uncredited + unpresented
        return mcq(3, f"নগদান বইয়ের ব্যাংক জের {bn(cash_book)} টাকা, ব্যাংকে জমা দেওয়া কিন্তু আদায় হয়নি এমন চেক {bn(uncredited)} টাকা এবং ইস্যুকৃত কিন্তু উপস্থাপিত হয়নি এমন চেক {bn(unpresented)} টাকা হলে পাসবইয়ের জের কত?",
                   f"{bn(passbook)} টাকা", [f"{bn(x)} টাকা" for x in {cash_book + uncredited - unpresented, cash_book + uncredited + unpresented, cash_book - uncredited - unpresented} - {passbook}][:3] + ([f"{bn(passbook + 1000)} টাকা"] if cash_book + uncredited - unpresented == passbook else []),
                   "পাসবই জের = নগদান বই জের − অনাদায়ী জমা + অনুপস্থাপিত চেক।", rng)
    if ch == 2:
        return rng.choice([
            mcq(2, "বাট্টা হিসাব কোন বইয়ের সাথে সম্পর্কিত?", "ত্রিঘরা নগদান বই", ["ক্রয় বই", "বিক্রয় বই", "ক্রয় ফেরত বই"], "ত্রিঘরা নগদান বইয়ে নগদ, ব্যাংক ও বাট্টার ঘর থাকে।", rng),
            mcq(2, "বাকিতে পণ্য ক্রয় কোন বইয়ে লেখা হয়?", "ক্রয় বই", ["নগদান বই", "বিক্রয় বই", "প্রকৃত জাবেদা"], "শুধু ধারে পণ্য ক্রয় ক্রয় বইয়ে লিপিবদ্ধ হয়।", rng),
        ])
    return rng.choice([
        mcq(1, "হিসাববিজ্ঞানের ভাষা বলা হয় কোনটিকে?", "ব্যবসায়ের ভাষা", ["অর্থনীতির ভাষা", "গণিতের ভাষা", "আইনের ভাষা"], "আর্থিক তথ্য সংশ্লিষ্ট পক্ষকে জানায় বলে হিসাববিজ্ঞানকে ব্যবসায়ের ভাষা বলে।", rng),
        mcq(1, "হিসাব সমীকরণের সঠিক রূপ কোনটি?", "সম্পদ = দায় + মালিকানাস্বত্ব", ["সম্পদ = দায় − মালিকানাস্বত্ব", "দায় = সম্পদ + মালিকানাস্বত্ব", "মালিকানাস্বত্ব = সম্পদ + দায়"], "প্রতিষ্ঠানের সম্পদ দায় ও মালিকানাস্বত্বের সমষ্টি।", rng),
    ])


def cq_hsc_accounting(rng):
    cb = rng.randint(30, 90) * 1000
    a, b, c = rng.randint(2, 9) * 1000, rng.randint(2, 9) * 1000, rng.randint(1, 3) * 500
    pb = cb - a + b - c
    return cq(3, f"৩১ ডিসেম্বর তারিখে রহিম ট্রেডার্সের নগদান বইয়ের ব্যাংক জের {bn(cb)} টাকা। (১) ব্যাংকে জমা দেওয়া চেক {bn(a)} টাকা আদায় হয়নি। (২) ইস্যুকৃত চেক {bn(b)} টাকা উপস্থাপিত হয়নি। (৩) ব্যাংক চার্জ {bn(c)} টাকা নগদান বইয়ে লেখা হয়নি।", [
        ("ব্যাংক সমন্বয় বিবরণী কী?", "নগদান বই ও পাসবইয়ের জেরের পার্থক্যের কারণ দেখিয়ে উভয়ের মধ্যে সমন্বয় সাধনের বিবরণী।"),
        ("নগদান বই ও পাসবইয়ের জের মেলে না কেন?", "সময়ের ব্যবধান, ভুল বা অজানা লেনদেনের কারণে দুই বইয়ের জের ভিন্ন হতে পারে।"),
        ("নগদান বইয়ের জের থেকে শুরু করে ব্যাংক সমন্বয় বিবরণী প্রস্তুত কর।", f"নগদান বই জের {bn(cb)} − অনাদায়ী জমা {bn(a)} + অনুপস্থাপিত চেক {bn(b)} − ব্যাংক চার্জ {bn(c)} = পাসবই জের {bn(pb)} টাকা।"),
        ("সংশোধিত নগদান বইয়ের জের নির্ণয় করে এর গুরুত্ব বিশ্লেষণ কর।", f"সংশোধিত জের = {bn(cb)} − {bn(c)} = {bn(cb - c)} টাকা; এটি প্রকৃত ব্যাংক জের প্রকাশ করে।"),
    ])


def g_hsc_economics(rng):
    ch = rng.randint(1, 3)
    if ch == 3:
        p1, p2 = rng.choice([(10, 12), (20, 25), (50, 40), (8, 10)])
        q1 = rng.randint(10, 50) * 10
        e = rng.choice([0.5, 1, 1.5, 2])
        dq = -e * (p2 - p1) / p1 * q1
        q2 = int(q1 + dq)
        elasticity = abs(((q2 - q1) / q1) / ((p2 - p1) / p1))
        ans = fmt(round(elasticity, 2))
        wrong = [fmt(round(elasticity * f, 2)) for f in (2, 0.5, 1.5) if fmt(round(elasticity * f, 2)) != ans]
        return mcq(3, f"কোনো দ্রব্যের দাম {bn(p1)} টাকা থেকে {bn(p2)} টাকা হলে চাহিদা {bn(q1)} একক থেকে {bn(q2)} একক হয়। চাহিদার দাম স্থিতিস্থাপকতা কত?", ans, wrong[:3],
                   "eₚ = (চাহিদার শতকরা পরিবর্তন) ÷ (দামের শতকরা পরিবর্তন)।", rng)
    if ch == 2:
        return rng.choice([
            mcq(2, "প্রান্তিক উপযোগ শূন্য হলে মোট উপযোগ কেমন হয়?", "সর্বোচ্চ", ["সর্বনিম্ন", "শূন্য", "ঋণাত্মক"], "প্রান্তিক উপযোগ শূন্য হলে মোট উপযোগ সর্বোচ্চ হয়।", rng),
            mcq(2, "ক্রমহ্রাসমান প্রান্তিক উপযোগ বিধি অনুযায়ী দ্রব্যের ভোগ বাড়লে প্রান্তিক উপযোগ:", "কমে", ["বাড়ে", "স্থির থাকে", "দ্বিগুণ হয়"], "অতিরিক্ত প্রতিটি এককের উপযোগ ক্রমশ কমতে থাকে।", rng),
        ])
    return rng.choice([
        mcq(1, "অর্থনীতির মৌলিক সমস্যার মূল কারণ কোনটি?", "সম্পদের দুষ্প্রাপ্যতা", ["অসীম সম্পদ", "জনসংখ্যা হ্রাস", "সরকারি নীতি"], "অসীম অভাবের তুলনায় সম্পদ সীমিত হওয়ায় নির্বাচনের সমস্যা দেখা দেয়।", rng),
        mcq(1, "সুযোগ ব্যয় বলতে কী বোঝায়?", "পরবর্তী সর্বোত্তম বিকল্প ত্যাগ", ["উৎপাদনের মোট ব্যয়", "বাজার দাম", "গড় ব্যয়"], "কোনো কিছু পেতে যে সর্বোত্তম বিকল্প ছাড়তে হয় তাই সুযোগ ব্যয়।", rng),
    ])


def cq_hsc_economics(rng):
    p1 = rng.choice([10, 20, 40, 50])
    p2 = p1 + p1 // rng.choice([2, 4, 5])
    q1 = rng.randint(5, 30) * 20
    q2 = q1 - q1 // rng.choice([2, 4, 5, 10])
    dq, dp = (q1 - q2) / q1, (p2 - p1) / p1
    e = round(dq / dp, 2)
    nature = "অস্থিতিস্থাপক; দাম বাড়ালে বিক্রেতার মোট আয় বাড়বে" if e < 1 else \
        "একক স্থিতিস্থাপক; দাম পরিবর্তনে মোট আয় অপরিবর্তিত থাকে" if e == 1 else "স্থিতিস্থাপক; দাম বাড়ালে বিক্রেতার মোট আয় কমবে"
    return cq(3, f"একটি দ্রব্যের দাম {bn(p1)} টাকা থেকে বেড়ে {bn(p2)} টাকা হলে এর চাহিদা {bn(q1)} একক থেকে কমে {bn(q2)} একক হয়।", [
        ("চাহিদা বিধি কী?", "অন্যান্য অবস্থা স্থির থাকলে দাম বাড়লে চাহিদা কমে এবং দাম কমলে চাহিদা বাড়ে।"),
        ("চাহিদা রেখা ডান দিকে নিম্নগামী হয় কেন?", "দাম ও চাহিদার বিপরীত সম্পর্ক, আয় প্রভাব ও পরিবর্তক প্রভাবের কারণে।"),
        ("চাহিদার দাম স্থিতিস্থাপকতা নির্ণয় কর।", f"eₚ = ({bn(q1 - q2)}/{bn(q1)}) ÷ ({bn(p2 - p1)}/{bn(p1)}) = {fmt(e)}।"),
        ("স্থিতিস্থাপকতার মান অনুযায়ী দ্রব্যটির প্রকৃতি বিশ্লেষণ কর।", f"eₚ = {fmt(e)}, তাই চাহিদা {nature}।"),
    ])



# ----------------------------------------------------------------------------- more numeric templates

def frac_text(f):
    return bn(f.numerator) if f.denominator == 1 else f"{bn(f.numerator)}/{bn(f.denominator)}"


def g_ssc_highermath(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        a, b = rng.randint(1, 5), rng.randint(-6, 6)
        x = rng.randint(-3, 4)
        ans = x * x * a + b * x - 2
        return mcq(1, f"f(x) = {bn(a)}x² + {bn(b)}x − ২ কে (x − {bn(x)}) দ্বারা ভাগ করলে ভাগশেষ কত?", bn(ans),
                   [bn(v) for v in near(rng, ans, positive=False)], f"ভাগশেষ উপপাদ্য অনুযায়ী ভাগশেষ = f({bn(x)}) = {bn(ans)}।", rng)
    if ch == 2:
        items = [("sin ৩০°", "১/২"), ("cos ৬০°", "১/২"), ("tan ৪৫°", "১"), ("sin ৯০°", "১"), ("cos ০°", "১"), ("tan ০°", "০"), ("sin ০°", "০")]
        name, val = rng.choice(items)
        pool = ["০", "১/২", "১", "√৩/২", "১/√২", "√৩"]
        return mcq(2, f"{name} এর মান কত?", val, [p for p in pool if p != val][:3] if rng.random() < 0.5 else rng.sample([p for p in pool if p != val], 3),
                   f"{name} = {val}।", rng)
    a, d, n = rng.randint(1, 9), rng.randint(2, 7), rng.randint(5, 20)
    if rng.random() < 0.5:
        s = n * (2 * a + (n - 1) * d) // 2
        return mcq(3, f"{bn(a)} + {bn(a + d)} + {bn(a + 2 * d)} + ... ধারাটির প্রথম {bn(n)}টি পদের সমষ্টি কত?", bn(s),
                   [bn(v) for v in near(rng, s)], f"Sₙ = n/২ {{২a + (n − ১)d}} = {bn(s)}।", rng)
    r = rng.choice([2, 3])
    k = rng.randint(4, 7)
    term = a * r ** (k - 1)
    return mcq(3, f"{bn(a)}, {bn(a * r)}, {bn(a * r * r)}, ... গুণোত্তর ধারাটির {bn(k)}তম পদ কত?", bn(term),
               [bn(v) for v in (a * r ** k, a * r ** (k - 2), term + a)], f"n-তম পদ = arⁿ⁻¹ = {bn(a)} × {bn(r)}^{bn(k - 1)} = {bn(term)}।", rng)


def cq_ssc_highermath(rng):
    a, d, n = rng.randint(2, 9), rng.randint(2, 6), rng.randint(10, 25)
    nth = a + (n - 1) * d
    s = n * (a + nth) // 2
    return cq(3, f"একটি সমান্তর ধারার প্রথম পদ {bn(a)} এবং সাধারণ অন্তর {bn(d)}।", [
        ("সমান্তর ধারা কাকে বলে?", "যে ধারার যেকোনো পদ ও তার পূর্ববর্তী পদের পার্থক্য সমান তাকে সমান্তর ধারা বলে।"),
        ("ধারাটির প্রথম তিনটি পদ লেখ।", f"{bn(a)}, {bn(a + d)}, {bn(a + 2 * d)}।"),
        (f"ধারাটির {bn(n)}তম পদ নির্ণয় কর।", f"a + (n − ১)d = {bn(a)} + {bn(n - 1)} × {bn(d)} = {bn(nth)}।"),
        (f"প্রথম {bn(n)}টি পদের সমষ্টি নির্ণয় কর।", f"Sₙ = n/২ (a + l) = {bn(n)}/২ × ({bn(a)} + {bn(nth)}) = {bn(s)}।"),
    ])


def g_ssc_finance(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        return rng.choice([
            mcq(1, "অর্থায়নের প্রধান কাজ কোনটি?", "তহবিল সংগ্রহ ও ব্যবহার", ["পণ্য উৎপাদন", "বিজ্ঞাপন দেওয়া", "কর্মী নিয়োগ"], "অর্থায়ন তহবিল সংগ্রহ, বিনিয়োগ ও লভ্যাংশ সিদ্ধান্ত নেয়।", rng),
            mcq(1, "ঝুঁকি ও আয়ের সম্পর্ক কেমন?", "ধনাত্মক", ["ঋণাত্মক", "সম্পর্কহীন", "সবসময় সমান"], "বেশি ঝুঁকিতে বেশি প্রত্যাশিত আয়।", rng),
        ])
    if ch == 2:
        p = rng.randint(1, 20) * 1000
        r = rng.choice([5, 6, 8, 10, 12])
        n = rng.randint(2, 5)
        if rng.random() < 0.5:
            si = p * r * n // 100
            return mcq(2, f"{bn(p)} টাকা {bn(r)}% সরল সুদে {bn(n)} বছরে কত সুদ হবে?", f"{bn(si)} টাকা",
                       [f"{bn(x)} টাকা" for x in near(rng, si, spread=max(100, si // 3))], f"সুদ = আসল × হার × সময় = {bn(si)} টাকা।", rng)
        fv = round(p * (1 + r / 100) ** n, 2)
        return mcq(2, f"{bn(p)} টাকা বার্ষিক {bn(r)}% চক্রবৃদ্ধি হারে {bn(n)} বছর পর কত হবে?", f"{fmt(fv)} টাকা",
                   [f"{fmt(x)} টাকা" for x in (round(p * (1 + r * n / 100), 2), round(fv * 1.05, 2), round(fv * 0.95, 2))],
                   f"FV = P(১ + r)ⁿ = {fmt(fv)} টাকা।", rng)
    return rng.choice([
        mcq(3, "বাংলাদেশের কেন্দ্রীয় ব্যাংকের নাম কী?", "বাংলাদেশ ব্যাংক", ["সোনালী ব্যাংক", "জনতা ব্যাংক", "অগ্রণী ব্যাংক"], "বাংলাদেশ ব্যাংক মুদ্রানীতি পরিচালনা করে।", rng),
        mcq(3, "কোন হিসাবে দিনে একাধিকবার লেনদেন করা যায়?", "চলতি হিসাব", ["স্থায়ী হিসাব", "সঞ্চয়ী হিসাব", "ডিপিএস"], "চলতি হিসাব ব্যবসায়ীদের জন্য উপযোগী।", rng),
    ])


def cq_ssc_finance(rng):
    p = rng.randint(5, 50) * 1000
    r = rng.choice([6, 8, 10, 12])
    n = rng.randint(2, 5)
    si = p * r * n / 100
    fv = round(p * (1 + r / 100) ** n, 2)
    return cq(2, f"রানা ব্যাংকে {bn(p)} টাকা বার্ষিক {bn(r)}% সুদে {bn(n)} বছরের জন্য জমা রাখলেন।", [
        ("অর্থের সময়মূল্য কী?", "আজকের এক টাকার মূল্য ভবিষ্যতের এক টাকার চেয়ে বেশি: এ ধারণাই অর্থের সময়মূল্য।"),
        ("চক্রবৃদ্ধি সুদ বলতে কী বোঝায়?", "আসলের সাথে অর্জিত সুদও পরবর্তী সময়ে সুদ অর্জন করে।"),
        ("সরল সুদে মেয়াদ শেষে রানা কত টাকা পাবেন?", f"আসল + সুদ = {bn(p)} + {fmt(si)} = {fmt(p + si)} টাকা।"),
        ("চক্রবৃদ্ধি সুদে কত পাবেন এবং কোনটি লাভজনক বিশ্লেষণ কর।", f"FV = {fmt(fv)} টাকা; চক্রবৃদ্ধিতে {fmt(round(fv - p - si, 2))} টাকা বেশি, তাই এটি লাভজনক।"),
    ])


def g_ssc_economics(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        return rng.choice([
            mcq(1, "অর্থনীতির জনক কাকে বলা হয়?", "অ্যাডাম স্মিথ", ["কার্ল মার্কস", "কেইনস", "মার্শাল"], "'Wealth of Nations' গ্রন্থের লেখক অ্যাডাম স্মিথ।", rng),
            mcq(1, "'Wealth of Nations' গ্রন্থটি প্রকাশিত হয় কোন সালে?", "১৭৭৬", ["১৭৮৯", "১৮৪৮", "১৯৩৬"], "অ্যাডাম স্মিথ ১৭৭৬ সালে গ্রন্থটি প্রকাশ করেন।", rng),
            mcq(1, "অর্থনীতির মৌলিক সমস্যার কারণ কী?", "সম্পদের দুষ্প্রাপ্যতা", ["অসীম সম্পদ", "জনসংখ্যা হ্রাস", "কম চাহিদা"], "অসীম অভাব কিন্তু সীমিত সম্পদ।", rng),
        ])
    if ch == 2:
        a, b, c, d = rng.randint(80, 150), rng.randint(2, 6), rng.randint(5, 30), rng.randint(2, 6)
        if (a - c) % (b + d):
            a += (b + d) - (a - c) % (b + d)
        p = (a - c) // (b + d)
        return mcq(2, f"চাহিদা Qd = {bn(a)} − {bn(b)}P এবং যোগান Qs = {bn(c)} + {bn(d)}P হলে ভারসাম্য দাম কত?", f"{bn(p)} টাকা",
                   [f"{bn(x)} টাকা" for x in near(rng, p)], f"Qd = Qs ⇒ {bn(a - c)} = {bn(b + d)}P ⇒ P = {bn(p)}।", rng)
    gdp = rng.randint(20, 90) * 1000
    pop = rng.choice([10, 20, 25, 40, 50])
    per = gdp * 1000 // pop
    return mcq(3, f"একটি দেশের জিডিপি {bn(gdp)} কোটি টাকা এবং জনসংখ্যা {bn(pop)} কোটি হলে মাথাপিছু আয় কত?", f"{bn(per // 1000)} টাকা",
               [f"{bn(x)} টাকা" for x in near(rng, per // 1000)], "মাথাপিছু আয় = জাতীয় আয় ÷ মোট জনসংখ্যা।", rng)


def cq_ssc_economics(rng):
    a, b, c, d = rng.randint(100, 160), rng.randint(2, 5), rng.randint(10, 30), rng.randint(2, 5)
    if (a - c) % (b + d):
        a += (b + d) - (a - c) % (b + d)
    p = (a - c) // (b + d)
    qty = a - b * p
    return cq(2, f"একটি দ্রব্যের চাহিদা অপেক্ষক Qd = {bn(a)} − {bn(b)}P এবং যোগান অপেক্ষক Qs = {bn(c)} + {bn(d)}P।", [
        ("ভারসাম্য দাম কাকে বলে?", "যে দামে চাহিদা ও যোগান সমান হয়।"),
        ("চাহিদা রেখা নিম্নগামী হয় কেন?", "দাম বাড়লে চাহিদা কমে: চাহিদার বিধি।"),
        ("ভারসাম্য দাম ও পরিমাণ নির্ণয় কর।", f"P = {bn(p)} টাকা এবং Q = {bn(qty)} একক।"),
        (f"দাম {bn(p + 2)} টাকা হলে বাজারে কী ঘটবে বিশ্লেষণ কর।", f"Qd = {bn(a - b * (p + 2))}, Qs = {bn(c + d * (p + 2))}; উদ্বৃত্ত যোগান তৈরি হয়ে দাম কমে ভারসাম্যে ফিরবে।"),
    ])


def g_hsc_physics_2(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        t1 = rng.choice([400, 500, 600, 800])
        t2 = rng.choice([200, 300])
        eff = round((1 - t2 / t1) * 100, 1)
        return mcq(1, f"একটি কার্নো ইঞ্জিনের উৎসের তাপমাত্রা {bn(t1)} K এবং তাপগ্রাহকের তাপমাত্রা {bn(t2)} K হলে দক্ষতা কত?", f"{fmt(eff)}%",
                   [f"{fmt(x)}%" for x in (round(t2 / t1 * 100, 1), round(eff / 2, 1), min(99.0, round(eff + 10, 1)))],
                   f"η = (১ − T₂/T₁) × ১০০% = {fmt(eff)}%।", rng)
    if ch == 2:
        q1, q2, r = rng.randint(1, 5), rng.randint(1, 5), rng.choice([1, 2, 3])
        f = 9 * q1 * q2 / (r * r)
        return mcq(2, f"শূন্য মাধ্যমে {bn(q1)} μC ও {bn(q2)} μC দুটি চার্জ {bn(r)} m দূরে থাকলে এদের মধ্যকার বল কত? (k = ৯ × ১০⁹)", f"{fmt(round(f / 1000, 4))} N",
                   [f"{fmt(round(f / 1000 * m, 4))} N" for m in (2, 0.5, 3)], f"F = kq₁q₂/r² = {fmt(round(f / 1000, 4))} N।", rng)
    v, r = rng.randint(2, 24), rng.choice([2, 3, 4, 5, 6, 8])
    if rng.random() < 0.5:
        i = round(v / r, 2)
        return mcq(3, f"{bn(r)} Ω রোধের দুই প্রান্তে {bn(v)} V বিভব পার্থক্য প্রয়োগ করলে প্রবাহ কত?", f"{fmt(i)} A",
                   [f"{fmt(x)} A" for x in (v * r, round(r / v, 2), round(i + 1, 2))], f"I = V/R = {fmt(i)} A।", rng)
    r1, r2 = rng.choice([(2, 2), (3, 6), (4, 4), (6, 12), (10, 15)])
    par = round(r1 * r2 / (r1 + r2), 2)
    return mcq(3, f"{bn(r1)} Ω ও {bn(r2)} Ω রোধ সমান্তরালে যুক্ত করলে তুল্য রোধ কত?", f"{fmt(par)} Ω",
               [f"{fmt(x)} Ω" for x in (r1 + r2, round((r1 + r2) / 2, 2), round(par + 1, 2))], f"১/R = ১/{bn(r1)} + ১/{bn(r2)} ⇒ R = {fmt(par)} Ω।", rng)


def cq_hsc_physics_2(rng):
    r1, r2, v = rng.choice([(3, 6, 12), (4, 4, 8), (6, 12, 24), (2, 3, 10)])
    series = r1 + r2
    par = round(r1 * r2 / (r1 + r2), 2)
    return cq(3, f"একটি বর্তনীতে {bn(r1)} Ω ও {bn(r2)} Ω রোধ এবং {bn(v)} V তড়িচ্চালক শক্তির কোষ রয়েছে।", [
        ("ওহমের সূত্র কী?", "তাপমাত্রা স্থির থাকলে পরিবাহীর প্রবাহ বিভব পার্থক্যের সমানুপাতিক।"),
        ("রোধ তাপমাত্রার ওপর নির্ভর করে কেন?", "তাপমাত্রা বাড়লে ইলেকট্রনের সংঘর্ষ বেড়ে রোধ বাড়ে।"),
        ("রোধ দুটি শ্রেণিতে যুক্ত করলে প্রবাহ নির্ণয় কর।", f"R = {bn(series)} Ω; I = V/R = {fmt(round(v / series, 2))} A।"),
        ("শ্রেণি ও সমান্তরাল সংযোগের প্রবাহের তুলনা বিশ্লেষণ কর।", f"সমান্তরালে R = {fmt(par)} Ω, I = {fmt(round(v / par, 2))} A; তুল্য রোধ কম বলে সমান্তরালে প্রবাহ বেশি।"),
    ])


COMPOUNDS = [("H₂O", "পানি", 18), ("CO₂", "কার্বন ডাই-অক্সাইড", 44), ("NaCl", "সোডিয়াম ক্লোরাইড", 58.5), ("CaCO₃", "ক্যালসিয়াম কার্বনেট", 100),
             ("NaOH", "সোডিয়াম হাইড্রক্সাইড", 40), ("H₂SO₄", "সালফিউরিক অ্যাসিড", 98), ("C₆H₁₂O₆", "গ্লুকোজ", 180), ("NH₃", "অ্যামোনিয়া", 17)]


def g_hsc_chemistry_2(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        k = rng.randint(1, 6)
        return mcq(1, f"একটি দ্রবণে [H⁺] = ১০⁻{str(k).translate(SUPER)} mol/L হলে pH কত?", bn(k),
                   [bn(x) for x in (14 - k, k + 1, max(0, k - 1)) if x != k][:3] + ([bn(k + 2)] if k == 1 else []),
                   f"pH = −log[H⁺] = {bn(k)}।", rng)
    if ch == 2:
        return rng.choice([
            mcq(2, "জৈব যৌগের মূল উপাদান কোনটি?", "কার্বন", ["নাইট্রোজেন", "সালফার", "সোডিয়াম"], "জৈব রসায়ন কার্বনের যৌগ নিয়ে আলোচনা করে।", rng),
            mcq(2, "মিথেনের আণবিক সংকেত কোনটি?", "CH₄", ["C₂H₆", "C₂H₄", "CO₂"], "মিথেন সরলতম অ্যালকেন।", rng),
            mcq(2, "অ্যালকেনের সাধারণ সংকেত কোনটি?", "CₙH₂ₙ₊₂", ["CₙH₂ₙ", "CₙH₂ₙ₋₂", "CₙHₙ"], "অ্যালকেন সম্পৃক্ত হাইড্রোকার্বন।", rng),
        ])
    formula, name, m = rng.choice(COMPOUNDS)
    moles = rng.choice([0.5, 1, 2, 3, 0.25])
    mass = round(m * moles, 2)
    return mcq(3, f"{fmt(mass)} g {name} ({formula}) এ কত মোল আছে?", f"{fmt(moles)} মোল",
               [f"{fmt(x)} মোল" for x in (moles * 2, round(moles / 2, 3), moles + 1)], f"মোল = ভর ÷ আণবিক ভর = {fmt(mass)} ÷ {fmt(m)} = {fmt(moles)}।", rng)


def cq_hsc_chemistry_2(rng):
    formula, name, m = rng.choice(COMPOUNDS[3:6])
    mass = rng.choice([4, 10, 20, 40])
    vol = rng.choice([250, 500, 1000])
    n = round(mass / m, 4)
    mol = round(n / (vol / 1000), 4)
    return cq(3, f"{bn(mass)} g {name} ({formula}) পানিতে দ্রবীভূত করে {bn(vol)} mL দ্রবণ তৈরি করা হলো।", [
        ("মোলারিটি কী?", "এক লিটার দ্রবণে দ্রবীভূত দ্রবের মোল সংখ্যা।"),
        ("প্রমাণ দ্রবণ বলতে কী বোঝায়?", "যে দ্রবণের ঘনমাত্রা নির্দিষ্টভাবে জানা।"),
        ("দ্রবের মোল সংখ্যা নির্ণয় কর।", f"n = {bn(mass)} ÷ {fmt(m)} = {fmt(n)} মোল।"),
        ("দ্রবণের মোলারিটি নির্ণয় করে বিশ্লেষণ কর।", f"M = {fmt(n)} ÷ {fmt(vol / 1000)} = {fmt(mol)} M; আয়তন কমালে মোলারিটি বাড়বে।"),
    ])


def g_hsc_biology_2(rng):
    ch = rng.randint(1, 3)
    if ch == 3 and rng.random() < 0.6:
        hr, sv = rng.choice([60, 70, 72, 75, 80]), rng.choice([60, 65, 70, 75])
        co = hr * sv
        return mcq(3, f"হৃৎস্পন্দন মিনিটে {bn(hr)} বার এবং স্ট্রোক ভলিউম {bn(sv)} mL হলে কার্ডিয়াক আউটপুট কত?", f"{bn(co)} mL/মিনিট",
                   [f"{bn(x)} mL/মিনিট" for x in near(rng, co, spread=600)], "কার্ডিয়াক আউটপুট = হৃৎস্পন্দন × স্ট্রোক ভলিউম।", rng)
    facts = {
        1: [("কোন প্রাণী পর্বের প্রাণীদের দেহ খণ্ডায়িত ও সন্ধিপদযুক্ত?", "আর্থ্রোপোডা", ["মলাস্কা", "পরিফেরা", "নিডারিয়া"], "আর্থ্রোপোডা পর্বের প্রাণীর পা সন্ধিযুক্ত।"),
            ("মানুষের বৈজ্ঞানিক নাম কী?", "Homo sapiens", ["Rana tigrina", "Panthera tigris", "Felis catus"], "মানুষ Hominidae গোত্রের Homo sapiens।"),
            ("দ্বিপদ নামকরণের প্রবর্তক কে?", "ক্যারোলাস লিনিয়াস", ["ডারউইন", "মেন্ডেল", "হুক"], "লিনিয়াস জীবের দুই অংশের বৈজ্ঞানিক নাম চালু করেন।")],
        2: [("মানবদেহের বৃহত্তম গ্রন্থি কোনটি?", "যকৃৎ", ["অগ্ন্যাশয়", "থাইরয়েড", "পিটুইটারি"], "যকৃৎ পিত্তরস তৈরি করে।"),
            ("পেপসিন এনজাইম কোথায় কাজ করে?", "পাকস্থলী", ["মুখগহ্বর", "ক্ষুদ্রান্ত্র", "বৃহদন্ত্র"], "পেপসিন আমিষ পরিপাক শুরু করে।"),
            ("পিত্তরস তৈরি হয় কোথায়?", "যকৃৎ", ["পিত্তথলি", "অগ্ন্যাশয়", "পাকস্থলী"], "পিত্তরস যকৃতে তৈরি হয়ে পিত্তথলিতে জমা থাকে।")],
        3: [("লোহিত রক্তকণিকার গড় আয়ু কত দিন?", "১২০ দিন", ["৩০ দিন", "৬০ দিন", "৩৬৫ দিন"], "লোহিত কণিকা প্রায় ১২০ দিন বাঁচে।"),
            ("সর্বজনীন দাতা রক্তের গ্রুপ কোনটি?", "O", ["AB", "A", "B"], "O গ্রুপে A বা B অ্যান্টিজেন নেই।"),
            ("সর্বজনীন গ্রহীতা রক্তের গ্রুপ কোনটি?", "AB", ["O", "A", "B"], "AB গ্রুপের প্লাজমায় অ্যান্টিবডি নেই।")],
    }
    qx, a, d, e = rng.choice(facts[ch])
    return mcq(ch, qx, a, d, e, rng)


def cq_hsc_biology_2(rng):
    hr, sv = rng.choice([60, 72, 80, 90]), rng.choice([60, 70, 80])
    return cq(3, f"একজন সুস্থ মানুষের হৃৎস্পন্দন মিনিটে {bn(hr)} বার এবং প্রতি স্পন্দনে {bn(sv)} mL রক্ত পাম্প হয়।", [
        ("হৃৎচক্র কী?", "একটি হৃৎস্পন্দনে সংঘটিত সংকোচন ও প্রসারণের ধারাবাহিক ঘটনা।"),
        ("মানুষের হৃৎপিণ্ডকে চার প্রকোষ্ঠবিশিষ্ট বলা হয় কেন?", "দুটি অলিন্দ ও দুটি নিলয় নিয়ে গঠিত।"),
        ("কার্ডিয়াক আউটপুট নির্ণয় কর।", f"{bn(hr)} × {bn(sv)} = {bn(hr * sv)} mL/মিনিট।"),
        ("ব্যায়ামের সময় কার্ডিয়াক আউটপুটের পরিবর্তন বিশ্লেষণ কর।", "ব্যায়ামে অক্সিজেনের চাহিদা বাড়ায় হৃৎস্পন্দন ও স্ট্রোক ভলিউম দুটোই বাড়ে, ফলে আউটপুট বাড়ে।"),
    ])


def g_hsc_highermath_2(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        r1, r2 = rng.randint(-6, 6), rng.randint(-6, 6)
        b, c = -(r1 + r2), r1 * r2
        eq = f"x² {'+' if b >= 0 else '−'} {bn(abs(b))}x {'+' if c >= 0 else '−'} {bn(abs(c))} = ০"
        if rng.random() < 0.5:
            return mcq(1, f"{eq} সমীকরণের মূলদ্বয়ের যোগফল কত?", bn(r1 + r2), [bn(v) for v in near(rng, r1 + r2, positive=False)],
                       "মূলদ্বয়ের যোগফল = −b/a।", rng)
        return mcq(1, f"{eq} সমীকরণের মূলদ্বয়ের গুণফল কত?", bn(c), [bn(v) for v in near(rng, c, positive=False)], "মূলদ্বয়ের গুণফল = c/a।", rng)
    if ch == 2:
        a, b = rng.choice([(3, 4), (5, 12), (6, 8), (8, 15), (1, 0), (0, 2)])
        mod = int(math.hypot(a, b))
        return mcq(2, f"z = {bn(a)} + {bn(b)}i হলে |z| এর মান কত?", bn(mod), [bn(v) for v in (a + b, abs(a - b) or mod + 2, mod + 1) if v != mod][:3] or [bn(mod + 3)],
                   f"|z| = √({bn(a)}² + {bn(b)}²) = {bn(mod)}।", rng)
    n = rng.randint(4, 9)
    r = rng.randint(1, n - 1)
    val = math.comb(n, r)
    return mcq(3, f"(১ + x)^{bn(n)} এর বিস্তৃতিতে x^{bn(r)} এর সহগ কত?", bn(val), [bn(v) for v in near(rng, val)], f"সহগ = ⁿCᵣ = {bn(n)}C{bn(r)} = {bn(val)}।", rng)


def cq_hsc_highermath_2(rng):
    n = rng.randint(5, 8)
    terms = [math.comb(n, r) for r in range(4)]
    return cq(3, f"(১ + x)^{bn(n)} একটি দ্বিপদী রাশি।", [
        ("দ্বিপদী রাশি কাকে বলে?", "দুটি পদ নিয়ে গঠিত বীজগাণিতিক রাশিকে দ্বিপদী রাশি বলে।"),
        ("বিস্তৃতিতে কয়টি পদ থাকবে ব্যাখ্যা কর।", f"(১ + x)ⁿ এর বিস্তৃতিতে n + ১ = {bn(n + 1)}টি পদ থাকে।"),
        ("বিস্তৃতির প্রথম চারটি পদ লেখ।", f"১ + {bn(terms[1])}x + {bn(terms[2])}x² + {bn(terms[3])}x³ + ..."),
        ("x = ০.১ বসিয়ে রাশির আসন্ন মান নির্ণয় কর।", f"≈ ১ + {fmt(round(terms[1] * 0.1, 3))} + {fmt(round(terms[2] * 0.01, 3))} + {fmt(round(terms[3] * 0.001, 3))} = {fmt(round(1 + terms[1] * 0.1 + terms[2] * 0.01 + terms[3] * 0.001, 3))}।"),
    ])


def g_hsc_accounting_2(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        a, b = rng.choice([(1, 1), (2, 1), (3, 2), (3, 1), (2, 3)])
        profit = rng.randint(5, 50) * 1000 * (a + b)
        share = profit * a // (a + b)
        return mcq(1, f"রহিম ও করিম {bn(a)}:{bn(b)} অনুপাতে লাভ-লোকসান ভাগ করেন। লাভ {bn(profit)} টাকা হলে রহিমের অংশ কত?", f"{bn(share)} টাকা",
                   [f"{bn(x)} টাকা" for x in (profit - share, profit // 2 + 1000, share + 1000) if x != share][:3], "অংশ = মোট লাভ × নিজস্ব অনুপাত ÷ মোট অনুপাত।", rng)
    if ch == 2:
        sales, cogs = rng.randint(50, 200) * 1000, rng.randint(20, 45) * 1000
        gp = sales - cogs
        return mcq(2, f"বিক্রয় {bn(sales)} টাকা এবং বিক্রীত পণ্যের ব্যয় {bn(cogs)} টাকা হলে মোট লাভ কত?", f"{bn(gp)} টাকা",
                   [f"{bn(x)} টাকা" for x in (sales + cogs, cogs, gp + 5000)], "মোট লাভ = বিক্রয় − বিক্রীত পণ্যের ব্যয়।", rng)
    ca, cl = rng.randint(4, 20) * 10000, rng.choice([2, 4, 5, 8]) * 10000
    ratio = round(ca / cl, 2)
    return mcq(3, f"চলতি সম্পদ {bn(ca)} টাকা এবং চলতি দায় {bn(cl)} টাকা হলে চলতি অনুপাত কত?", f"{fmt(ratio)} : ১",
               [f"{fmt(x)} : ১" for x in (round(cl / ca, 2), round(ratio + 1, 2), round(ratio * 2, 2))], "চলতি অনুপাত = চলতি সম্পদ ÷ চলতি দায়।", rng)


def cq_hsc_accounting_2(rng):
    a, b = rng.choice([(3, 2), (2, 1), (1, 1)])
    profit = rng.randint(10, 40) * 1000 * (a + b)
    sa, sb = profit * a // (a + b), profit * b // (a + b)
    return cq(1, f"রনি ও সনি একটি অংশীদারি ব্যবসায়ের অংশীদার। তারা {bn(a)}:{bn(b)} অনুপাতে লাভ-লোকসান ভাগ করেন। বছর শেষে লাভ {bn(profit)} টাকা।", [
        ("অংশীদারি চুক্তি কী?", "অংশীদারদের মধ্যে অধিকার, দায়িত্ব ও লাভ বণ্টনের লিখিত বা মৌখিক চুক্তি।"),
        ("চুক্তি না থাকলে লাভ কীভাবে বণ্টিত হয়?", "অংশীদারি আইন অনুযায়ী সমান হারে।"),
        ("প্রত্যেক অংশীদারের লাভের অংশ নির্ণয় কর।", f"রনি {bn(sa)} টাকা এবং সনি {bn(sb)} টাকা।"),
        ("চুক্তি না থাকলে বণ্টনে কী পরিবর্তন হতো বিশ্লেষণ কর।", f"সমানভাবে প্রত্যেকে {bn(profit // 2)} টাকা পেতেন; বেশি মূলধন দেওয়া অংশীদার ক্ষতিগ্রস্ত হতেন।"),
    ])


def g_hsc_finance_1(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        return rng.choice([
            mcq(1, "আর্থিক ব্যবস্থাপনার মূল লক্ষ্য কী?", "শেয়ারহোল্ডারদের সম্পদ সর্বাধিকরণ", ["কর্মী সংখ্যা বৃদ্ধি", "বিক্রয় কমানো", "দায় বৃদ্ধি"], "আধুনিক অর্থায়নে সম্পদ সর্বাধিকরণই লক্ষ্য।", rng),
            mcq(1, "অর্থায়নের সিদ্ধান্ত কয় প্রকার?", "তিন প্রকার", ["দুই প্রকার", "চার প্রকার", "পাঁচ প্রকার"], "বিনিয়োগ, অর্থসংস্থান ও লভ্যাংশ সিদ্ধান্ত।", rng),
        ])
    if ch == 2:
        fv = rng.randint(10, 100) * 1000
        r = rng.choice([5, 8, 10, 12])
        n = rng.randint(1, 4)
        pv = round(fv / (1 + r / 100) ** n, 2)
        return mcq(2, f"{bn(n)} বছর পর {bn(fv)} টাকা পেতে বার্ষিক {bn(r)}% হারে বর্তমান মূল্য কত?", f"{fmt(pv)} টাকা",
                   [f"{fmt(x)} টাকা" for x in (fv, round(fv * (1 - r * n / 100), 2), round(pv * 1.1, 2))], f"PV = FV ÷ (১ + r)ⁿ = {fmt(pv)} টাকা।", rng)
    inv = rng.randint(5, 30) * 10000
    cf = rng.choice([2, 4, 5]) * 10000
    pb = round(inv / cf, 2)
    return mcq(3, f"একটি প্রকল্পে বিনিয়োগ {bn(inv)} টাকা এবং বার্ষিক নগদ প্রবাহ {bn(cf)} টাকা হলে পরিশোধকাল কত?", f"{fmt(pb)} বছর",
               [f"{fmt(x)} বছর" for x in (round(pb + 1, 2), round(pb / 2, 2), round(cf / inv, 2))], "পরিশোধকাল = প্রাথমিক বিনিয়োগ ÷ বার্ষিক নগদ প্রবাহ।", rng)


def cq_hsc_finance_1(rng):
    inv = rng.randint(10, 30) * 10000
    flows = [rng.randint(3, 9) * 10000 for _ in range(3)]
    r = rng.choice([8, 10, 12])
    pv = sum(f / (1 + r / 100) ** (i + 1) for i, f in enumerate(flows))
    npv = round(pv - inv, 2)
    return cq(3, f"একটি প্রকল্পে প্রাথমিক বিনিয়োগ {bn(inv)} টাকা। পরবর্তী তিন বছরের নগদ প্রবাহ যথাক্রমে {', '.join(bn(f) for f in flows)} টাকা। মূলধন ব্যয় {bn(r)}%।", [
        ("মূলধন বাজেটিং কী?", "দীর্ঘমেয়াদি বিনিয়োগ প্রকল্প মূল্যায়ন ও নির্বাচনের প্রক্রিয়া।"),
        ("নিট বর্তমান মূল্য পদ্ধতি সময়মূল্য বিবেচনা করে কেন?", "ভবিষ্যৎ নগদ প্রবাহকে বাট্টা করে বর্তমান মূল্যে আনে।"),
        ("নগদ প্রবাহের মোট বর্তমান মূল্য নির্ণয় কর।", f"PV = {fmt(round(pv, 2))} টাকা।"),
        ("NPV নির্ণয় করে প্রকল্পটি গ্রহণযোগ্য কি না বিশ্লেষণ কর।", f"NPV = {fmt(npv)} টাকা; " + ("ধনাত্মক, তাই প্রকল্পটি গ্রহণযোগ্য।" if npv > 0 else "ঋণাত্মক, তাই প্রকল্পটি বর্জনীয়।")),
    ])


def g_hsc_marketing_1(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        return rng.choice([
            mcq(1, "উৎপাদনের উপাদান কয়টি?", "চারটি", ["তিনটি", "পাঁচটি", "দুইটি"], "ভূমি, শ্রম, মূলধন ও সংগঠন।", rng),
            mcq(1, "উৎপাদন বলতে কী বোঝায়?", "উপযোগ সৃষ্টি", ["পণ্য ধ্বংস", "শুধু বিক্রয়", "শুধু ক্রয়"], "অর্থনীতিতে উপযোগ সৃষ্টিই উৎপাদন।", rng),
        ])
    if ch == 2:
        out, inp = rng.randint(200, 900), rng.choice([10, 20, 25, 40])
        prod = round(out / inp, 2)
        return mcq(2, f"{bn(inp)} জন শ্রমিক দিনে {bn(out)} একক পণ্য উৎপাদন করলে শ্রমিকপ্রতি উৎপাদনশীলতা কত?", f"{fmt(prod)} একক",
                   [f"{fmt(x)} একক" for x in (out * inp, round(inp / out, 3), round(prod + 5, 2))], "উৎপাদনশীলতা = উৎপাদন ÷ উপকরণ।", rng)
    fc = rng.randint(2, 20) * 10000
    p, vc = rng.choice([(50, 30), (100, 60), (80, 40), (25, 15)])
    be = fc // (p - vc)
    return mcq(3, f"স্থির ব্যয় {bn(fc)} টাকা, একক বিক্রয়মূল্য {bn(p)} টাকা এবং একক পরিবর্তনশীল ব্যয় {bn(vc)} টাকা হলে সমচ্ছেদ বিন্দু কত একক?", f"{bn(be)} একক",
               [f"{bn(x)} একক" for x in near(rng, be, spread=max(50, be // 3))], "সমচ্ছেদ বিন্দু = স্থির ব্যয় ÷ (বিক্রয়মূল্য − পরিবর্তনশীল ব্যয়)।", rng)


def cq_hsc_marketing_1(rng):
    fc = rng.randint(5, 20) * 10000
    p, vc = rng.choice([(50, 30), (100, 60), (80, 40)])
    be = fc // (p - vc)
    target = be + rng.randint(1, 5) * 500
    return cq(3, f"একটি কারখানার স্থির ব্যয় {bn(fc)} টাকা। প্রতি একক পণ্যের বিক্রয়মূল্য {bn(p)} টাকা এবং পরিবর্তনশীল ব্যয় {bn(vc)} টাকা।", [
        ("সমচ্ছেদ বিন্দু কী?", "যে বিক্রয় পরিমাণে মোট আয় ও মোট ব্যয় সমান, লাভ বা ক্ষতি শূন্য।"),
        ("স্থির ব্যয় ও পরিবর্তনশীল ব্যয়ের পার্থক্য লেখ।", "স্থির ব্যয় উৎপাদনের সাথে বদলায় না, পরিবর্তনশীল ব্যয় উৎপাদনের সাথে বদলায়।"),
        ("সমচ্ছেদ বিন্দু নির্ণয় কর।", f"{bn(fc)} ÷ ({bn(p)} − {bn(vc)}) = {bn(be)} একক।"),
        (f"{bn(target)} একক বিক্রয় করলে লাভ কত হবে বিশ্লেষণ কর।", f"লাভ = ({bn(target)} − {bn(be)}) × {bn(p - vc)} = {bn((target - be) * (p - vc))} টাকা।"),
    ])


def g_hsc_geography_2(rng):
    ch = rng.randint(1, 3)
    if ch == 1:
        pop = rng.randint(20, 200) * 100000
        area = rng.choice([1000, 2000, 2500, 4000, 5000])
        dens = pop // area
        return mcq(1, f"একটি জেলার জনসংখ্যা {bn(pop)} জন এবং আয়তন {bn(area)} বর্গকিমি হলে জনসংখ্যার ঘনত্ব কত?", f"{bn(dens)} জন/বর্গকিমি",
                   [f"{bn(x)} জন/বর্গকিমি" for x in near(rng, dens, spread=max(50, dens // 3))], "ঘনত্ব = মোট জনসংখ্যা ÷ মোট আয়তন।", rng)
    if ch == 2:
        return rng.choice([
            mcq(2, "নগরায়ণের প্রধান কারণ কোনটি?", "কর্মসংস্থানের সুযোগ", ["কৃষিজমি বৃদ্ধি", "বনভূমি বৃদ্ধি", "নদীভাঙন হ্রাস"], "শহরে কাজের সুযোগ মানুষকে আকর্ষণ করে।", rng),
            mcq(2, "গ্রামীণ বসতির একটি ধরন কোনটি?", "রৈখিক বসতি", ["মহানগর", "উপশহর", "শিল্পনগরী"], "নদী বা রাস্তার পাশে লম্বা সারিতে গড়ে ওঠা বসতি।", rng),
        ])
    return rng.choice([
        mcq(3, "বাংলাদেশে কয়লা খনি কোথায় অবস্থিত?", "বড়পুকুরিয়া", ["মধ্যপাড়া", "হরিপুর", "জাফলং"], "দিনাজপুরের বড়পুকুরিয়ায় কয়লা খনি।", rng),
        mcq(3, "বাংলাদেশের একমাত্র কঠিন শিলার খনি কোথায়?", "মধ্যপাড়া", ["বড়পুকুরিয়া", "ভোলাগঞ্জ", "টেকেরহাট"], "দিনাজপুরের মধ্যপাড়ায় কঠিন শিলা উত্তোলন হয়।", rng),
        mcq(3, "বাংলাদেশে প্রথম প্রাকৃতিক গ্যাস আবিষ্কৃত হয় কোথায়?", "হরিপুর, সিলেট", ["তিতাস", "বিবিয়ানা", "কৈলাসটিলা"], "১৯৫৫ সালে সিলেটের হরিপুরে প্রথম গ্যাস পাওয়া যায়।", rng),
    ])


def cq_hsc_geography_2(rng):
    a_pop, a_area = rng.randint(30, 90) * 100000, rng.choice([2000, 2500, 3000])
    b_pop, b_area = rng.randint(10, 40) * 100000, rng.choice([4000, 5000, 6000])
    da, db = a_pop // a_area, b_pop // b_area
    return cq(1, f"'ক' জেলার জনসংখ্যা {bn(a_pop)} জন ও আয়তন {bn(a_area)} বর্গকিমি; 'খ' জেলার জনসংখ্যা {bn(b_pop)} জন ও আয়তন {bn(b_area)} বর্গকিমি।", [
        ("জনসংখ্যার ঘনত্ব কী?", "প্রতি বর্গকিলোমিটারে বসবাসকারী মানুষের সংখ্যা।"),
        ("জনসংখ্যা বণ্টনে ভূপ্রকৃতির প্রভাব লেখ।", "সমতল ও উর্বর অঞ্চলে বেশি, পাহাড়ি ও দুর্গম অঞ্চলে কম মানুষ বাস করে।"),
        ("দুই জেলার জনসংখ্যার ঘনত্ব নির্ণয় কর।", f"'ক' = {bn(da)} জন/বর্গকিমি, 'খ' = {bn(db)} জন/বর্গকিমি।"),
        ("ঘনত্বের পার্থক্যের সম্ভাব্য কারণ বিশ্লেষণ কর।", "উর্বর মাটি, শিল্প, যোগাযোগ ও কর্মসংস্থানের সুযোগ যেখানে বেশি সেখানে ঘনত্ব বেশি হয়।"),
    ])


def g_hsc_psychology(chapters_facts):
    def gen(rng):
        ch = rng.randint(1, 3)
        if ch == 2 and rng.random() < 0.6:
            ca = rng.randint(8, 16)
            ma = ca + rng.randint(-3, 4)
            iq = round(ma / ca * 100)
            return mcq(2, f"একজনের মানসিক বয়স {bn(ma)} বছর এবং প্রকৃত বয়স {bn(ca)} বছর হলে বুদ্ধ্যঙ্ক কত?", bn(iq),
                       [bn(v) for v in near(rng, iq, spread=15)], "বুদ্ধ্যঙ্ক = (মানসিক বয়স ÷ প্রকৃত বয়স) × ১০০।", rng)
        qx, a, d, e = rng.choice(chapters_facts[ch])
        return mcq(ch, qx, a, d, e, rng)
    return gen


PSYCHOLOGY_1 = {
    1: [("মনোবিজ্ঞানের জনক কাকে বলা হয়?", "উইলহেম উন্ট", ["ফ্রয়েড", "পাভলভ", "স্কিনার"], "উন্ট ১৮৭৯ সালে লাইপজিগে প্রথম মনোবিজ্ঞান গবেষণাগার স্থাপন করেন।"),
        ("প্রথম মনোবিজ্ঞান গবেষণাগার কোথায় স্থাপিত হয়?", "লাইপজিগ", ["লন্ডন", "প্যারিস", "ভিয়েনা"], "১৮৭৯ সালে জার্মানির লাইপজিগে।")],
    2: [("ক্লাসিক্যাল কন্ডিশনিং তত্ত্বের প্রবক্তা কে?", "আইভান পাভলভ", ["বি. এফ. স্কিনার", "ফ্রয়েড", "মাসলো"], "পাভলভ কুকুরের ওপর পরীক্ষা করেন।"),
        ("অপারেন্ট কন্ডিশনিং তত্ত্বের প্রবক্তা কে?", "বি. এফ. স্কিনার", ["পাভলভ", "থর্নডাইক", "উন্ট"], "স্কিনার পুরস্কার ও শাস্তির মাধ্যমে শিক্ষণ ব্যাখ্যা করেন।")],
    3: [("মাসলোর চাহিদা সোপানের সর্বোচ্চ স্তর কোনটি?", "আত্মবাস্তবায়ন", ["নিরাপত্তা", "সামাজিক চাহিদা", "শারীরবৃত্তীয় চাহিদা"], "নিজের সম্ভাবনার পূর্ণ বিকাশই আত্মবাস্তবায়ন।"),
        ("আবেগের জেমস-ল্যাঙ্গ তত্ত্ব অনুযায়ী আগে ঘটে কোনটি?", "শারীরিক পরিবর্তন", ["আবেগ অনুভূতি", "চিন্তা", "স্মৃতি"], "শারীরিক পরিবর্তনের অনুভূতিই আবেগ।")],
}
PSYCHOLOGY_2 = {
    1: [("বুদ্ধ্যঙ্ক (IQ) ধারণার প্রবর্তক কে?", "উইলিয়াম স্টার্ন", ["বিনে", "টারম্যান", "গার্ডনার"], "স্টার্ন মানসিক বয়স ও প্রকৃত বয়সের অনুপাতের ধারণা দেন।"),
        ("বহুমুখী বুদ্ধির তত্ত্বের প্রবক্তা কে?", "হাওয়ার্ড গার্ডনার", ["স্পিয়ারম্যান", "থার্স্টোন", "উন্ট"], "গার্ডনার একাধিক ধরনের বুদ্ধির কথা বলেন।")],
    2: [("মনঃসমীক্ষণ তত্ত্বের প্রবক্তা কে?", "সিগমুন্ড ফ্রয়েড", ["কার্ল ইয়ুং", "অ্যাডলার", "মাসলো"], "ফ্রয়েড অবচেতন মনের ধারণা দেন।"),
        ("ফ্রয়েডের মতে ব্যক্তিত্বের উপাদান কয়টি?", "তিনটি", ["দুইটি", "চারটি", "পাঁচটি"], "ইড, ইগো ও সুপারইগো।")],
    3: [("মানসিক চাপের একটি শারীরিক লক্ষণ কোনটি?", "হৃৎস্পন্দন বৃদ্ধি", ["ক্ষুধা বৃদ্ধি সবসময়", "উচ্চতা বৃদ্ধি", "দৃষ্টিশক্তি বৃদ্ধি"], "চাপে সিমপ্যাথেটিক স্নায়ু সক্রিয় হয়।"),
        ("মানসিক স্বাস্থ্য ভালো রাখার উপায় কোনটি?", "নিয়মিত ঘুম ও ব্যায়াম", ["একাকী থাকা", "রাত জাগা", "খাবার বাদ দেওয়া"], "ঘুম ও ব্যায়াম মানসিক চাপ কমায়।")],
}


def cq_hsc_psychology(rng):
    ca = rng.randint(8, 14)
    ma = ca + rng.randint(-2, 4)
    iq = round(ma / ca * 100)
    level = "গড়ের ওপরে" if iq > 110 else "গড়ের নিচে" if iq < 90 else "গড়"
    return cq(2, f"{bn(ca)} বছর বয়সী রাফির বুদ্ধি অভীক্ষায় মানসিক বয়স {bn(ma)} বছর পাওয়া গেল।", [
        ("বুদ্ধ্যঙ্ক কী?", "মানসিক বয়সকে প্রকৃত বয়স দিয়ে ভাগ করে ১০০ দিয়ে গুণ করে প্রাপ্ত মান।"),
        ("মানসিক বয়স বলতে কী বোঝায়?", "ব্যক্তির মানসিক সামর্থ্য যে বয়সের গড় মানুষের সমান।"),
        ("রাফির বুদ্ধ্যঙ্ক নির্ণয় কর।", f"({bn(ma)} ÷ {bn(ca)}) × ১০০ = {bn(iq)}।"),
        ("রাফির বুদ্ধির স্তর বিশ্লেষণ কর।", f"IQ {bn(iq)} হওয়ায় সে {level} বুদ্ধিসম্পন্ন; উপযুক্ত পরিবেশ ও শিক্ষায় সম্ভাবনা আরও বিকশিত হতে পারে।"),
    ])


def g_hsc_statistics_1(rng):
    ch = rng.randint(1, 3)
    data = sorted(rng.randint(2, 40) for _ in range(rng.choice([5, 7])))
    text = ", ".join(bn(v) for v in data)
    if ch == 1:
        rng_val = data[-1] - data[0]
        return mcq(1, f"উপাত্ত: {text}: পরিসর কত?", bn(rng_val), [bn(v) for v in near(rng, rng_val)], "পরিসর = বৃহত্তম মান − ক্ষুদ্রতম মান।", rng)
    if ch == 2:
        mean = round(sum(data) / len(data), 2)
        return mcq(2, f"উপাত্ত: {text}: গাণিতিক গড় কত?", fmt(mean), [fmt(round(mean + d, 2)) for d in (1.5, -2, 3)], "গড় = মোট যোগফল ÷ সংখ্যা।", rng)
    med = data[len(data) // 2]
    return mcq(3, f"উপাত্ত: {text}: মধ্যমা কত?", bn(med), [bn(v) for v in near(rng, med)], "মানগুলো ছোট থেকে বড় সাজালে মাঝের মানই মধ্যমা।", rng)


def cq_hsc_statistics_1(rng):
    data = [rng.randint(30, 95) for _ in range(6)]
    mean = round(sum(data) / 6, 2)
    s = sorted(data)
    med = round((s[2] + s[3]) / 2, 2)
    return cq(2, f"৬ জন শিক্ষার্থীর পরিসংখ্যানে প্রাপ্ত নম্বর: {', '.join(bn(v) for v in data)}।", [
        ("কেন্দ্রীয় প্রবণতা কী?", "উপাত্তের মানগুলো কোনো কেন্দ্রীয় মানের চারপাশে পুঞ্জীভূত হওয়ার প্রবণতা।"),
        ("গড়ের একটি সুবিধা লেখ।", "সব মান বিবেচনা করে এবং গাণিতিক বিশ্লেষণে সহজে ব্যবহার করা যায়।"),
        ("গাণিতিক গড় নির্ণয় কর।", f"গড় = {bn(sum(data))} ÷ ৬ = {fmt(mean)}।"),
        ("মধ্যমা নির্ণয় করে গড়ের সাথে তুলনা বিশ্লেষণ কর।", f"মধ্যমা = ({bn(s[2])} + {bn(s[3])}) ÷ ২ = {fmt(med)}; গড় ও মধ্যমার পার্থক্য উপাত্তের বিন্যাসের অসমতা নির্দেশ করে।"),
    ])


def g_hsc_statistics_2(rng):
    from fractions import Fraction
    ch = rng.randint(1, 3)
    if ch == 1:
        event, fav = rng.choice([("জোড় সংখ্যা", 3), ("৪ এর বেশি", 2), ("মৌলিক সংখ্যা", 3), ("৬", 1), ("৩ এর গুণিতক", 2)])
        p = Fraction(fav, 6)
        others = {Fraction(fav + 1, 6), Fraction(1, 2) if p != Fraction(1, 2) else Fraction(1, 3), Fraction(fav, 12)} - {p}
        return mcq(1, f"একটি নিরপেক্ষ ছক্কা একবার নিক্ষেপ করলে {event} পাওয়ার সম্ভাবনা কত?", frac_text(p), [frac_text(o) for o in others][:3],
                   f"অনুকূল ফলাফল {bn(fav)}টি, মোট ৬টি; সম্ভাবনা = {frac_text(p)}।", rng)
    if ch == 2:
        n = rng.randint(2, 4)
        total = 2 ** n
        return mcq(2, f"{bn(n)}টি মুদ্রা একসাথে নিক্ষেপ করলে নমুনাক্ষেত্রে মোট কয়টি ফলাফল থাকবে?", bn(total), [bn(v) for v in (2 * n, total - 1, total + 2)],
                   f"মোট ফলাফল = ২ⁿ = {bn(total)}।", rng)
    n, r = rng.randint(4, 9), rng.randint(2, 3)
    val = math.comb(n, r)
    return mcq(3, f"{bn(n)} জন থেকে {bn(r)} জনের একটি কমিটি কত উপায়ে গঠন করা যায়?", bn(val), [bn(v) for v in near(rng, val)], f"ⁿCᵣ = {bn(n)}C{bn(r)} = {bn(val)}।", rng)


def cq_hsc_statistics_2(rng):
    from fractions import Fraction
    red, blue = rng.randint(2, 6), rng.randint(2, 6)
    total = red + blue
    p_red = Fraction(red, total)
    p_two = Fraction(red * (red - 1), total * (total - 1))
    return cq(1, f"একটি থলিতে {bn(red)}টি লাল ও {bn(blue)}টি নীল বল আছে। দৈবভাবে বল তোলা হলো।", [
        ("সম্ভাবনা কী?", "কোনো ঘটনা ঘটার নিশ্চয়তার মাত্রা, যার মান ০ থেকে ১।"),
        ("পরস্পর বর্জনশীল ঘটনা বলতে কী বোঝায়?", "যে ঘটনাগুলো একসাথে ঘটতে পারে না।"),
        ("একটি বল তুললে লাল হওয়ার সম্ভাবনা নির্ণয় কর।", f"P(লাল) = {bn(red)}/{bn(total)} = {frac_text(p_red)}।"),
        ("প্রতিস্থাপন ছাড়া দুটি বল তুললে দুটিই লাল হওয়ার সম্ভাবনা বিশ্লেষণ কর।", f"P = ({bn(red)}/{bn(total)}) × ({bn(red - 1)}/{bn(total - 1)}) = {frac_text(p_two)}; প্রথম বল ফেরত না দেওয়ায় দ্বিতীয় ঘটনা নির্ভরশীল।"),
    ])


def pool_generators(pool):
    """Builds (mcq, repeat, cq) generators from a hand-written pool."""
    def gen(rng):
        chapter = rng.choice(sorted(pool["mcq"]))
        question, correct, distractors, explanation = rng.choice(pool["mcq"][chapter])
        return mcq(chapter, question, correct, distractors, explanation, rng)

    def cq_gen(rng):
        chapter, stem, parts = rng.choice(pool["cq"])
        return cq(chapter, stem, parts)

    return gen, gen, cq_gen


GENERATORS = {
    "ssc-math": (g_ssc_math, g_ssc_math_fixed, cq_ssc_math),
    "ssc-bgs": (g_ssc_bgs, g_ssc_bgs, cq_ssc_bgs),
    "ssc-ict": (g_ssc_ict, g_ssc_ict_fixed, cq_ssc_ict),
    "ssc-physics": (g_ssc_physics, g_ssc_physics_fixed, cq_ssc_physics),
    "ssc-chemistry": (g_ssc_chemistry, g_ssc_chemistry, cq_ssc_chemistry),
    "ssc-biology": (g_ssc_biology, g_ssc_biology_fixed, cq_ssc_biology),
    "ssc-highermath": (g_ssc_highermath, g_ssc_highermath, cq_ssc_highermath),
    "ssc-accounting": (g_ssc_accounting, g_ssc_accounting, cq_ssc_accounting),
    "ssc-finance": (g_ssc_finance, g_ssc_finance, cq_ssc_finance),
    "ssc-economics": (g_ssc_economics, g_ssc_economics, cq_ssc_economics),
    "hsc-ict": (g_hsc_ict, g_hsc_ict, cq_hsc_ict),
    "hsc-physics-1": (g_hsc_physics, g_hsc_physics, cq_hsc_physics),
    "hsc-physics-2": (g_hsc_physics_2, g_hsc_physics_2, cq_hsc_physics_2),
    "hsc-chemistry-1": (g_hsc_chemistry, g_hsc_chemistry, cq_hsc_chemistry),
    "hsc-chemistry-2": (g_hsc_chemistry_2, g_hsc_chemistry_2, cq_hsc_chemistry_2),
    "hsc-biology-1": (g_hsc_biology, g_hsc_biology, cq_hsc_biology),
    "hsc-biology-2": (g_hsc_biology_2, g_hsc_biology_2, cq_hsc_biology_2),
    "hsc-highermath-1": (g_hsc_highermath, g_hsc_highermath, cq_hsc_highermath),
    "hsc-highermath-2": (g_hsc_highermath_2, g_hsc_highermath_2, cq_hsc_highermath_2),
    "hsc-accounting-1": (g_hsc_accounting, g_hsc_accounting, cq_hsc_accounting),
    "hsc-accounting-2": (g_hsc_accounting_2, g_hsc_accounting_2, cq_hsc_accounting_2),
    "hsc-finance-1": (g_hsc_finance_1, g_hsc_finance_1, cq_hsc_finance_1),
    "hsc-marketing-1": (g_hsc_marketing_1, g_hsc_marketing_1, cq_hsc_marketing_1),
    "hsc-economics-1": (g_hsc_economics, g_hsc_economics, cq_hsc_economics),
    "hsc-geography-2": (g_hsc_geography_2, g_hsc_geography_2, cq_hsc_geography_2),
    "hsc-psychology-1": (g_hsc_psychology(PSYCHOLOGY_1), g_hsc_psychology(PSYCHOLOGY_1), cq_hsc_psychology),
    "hsc-psychology-2": (g_hsc_psychology(PSYCHOLOGY_2), g_hsc_psychology(PSYCHOLOGY_2), cq_hsc_psychology),
    "hsc-statistics-1": (g_hsc_statistics_1, g_hsc_statistics_1, cq_hsc_statistics_1),
    "hsc-statistics-2": (g_hsc_statistics_2, g_hsc_statistics_2, cq_hsc_statistics_2),
}
for _subject_id, _pool in {**POOLS_SSC, **POOLS_HSC}.items():
    GENERATORS[_subject_id] = pool_generators(_pool)


# ----------------------------------------------------------------------------- assembly

def paper_exists(exam, group, board, year):
    if exam == "HSC" and year == 2020:
        return False
    if year == 2021 and group in ("COMMON", "OPTIONAL"):
        return False
    if board == "mymensingh" and year < MYMENSINGH_FIRST_YEAR:
        return False
    return True


def content_key(q):
    if q["type"] == "MCQ":
        return ("MCQ", q["prompt"], tuple(sorted(o["text"] for o in q["options"])))
    return ("CQ", q["stem"], tuple(p["text"] for p in q["parts"]))


def build():
    questions = {}   # content key -> question dict
    for subject_id, exam, _, _, _, group, chapters in SUBJECTS:
        gen, fixed, cq_gen = GENERATORS[subject_id]
        for board, *_ in BOARDS:
            for year in YEARS:
                if not paper_exists(exam, group, board, year):
                    continue
                rng = random.Random(f"{subject_id}|{board}|{year}")
                paper, number, guard = [], 1, 0
                while len([q for q in paper if q["type"] == "MCQ"]) < MCQ_PER_PAPER and guard < 50:
                    guard += 1
                    try:
                        q = fixed(rng) if rng.random() < REPEAT_SHARE else gen(rng)
                    except Retry:
                        continue
                    if content_key(q) not in {content_key(p) for p in paper}:
                        paper.append(q)
                while len([q for q in paper if q["type"] == "CQ"]) < CQ_PER_PAPER and guard < 100:
                    guard += 1
                    q = cq_gen(rng)
                    if content_key(q) not in {content_key(p) for p in paper}:
                        paper.append(q)
                for q in paper:
                    key = (subject_id,) + content_key(q)
                    stored = questions.get(key)
                    if stored is None:
                        stored = dict(q, subjectId=subject_id, appearances=[])
                        stored["chapterId"] = f"{subject_id}-{q['chapter']}"
                        questions[key] = stored
                    stored["appearances"].append(f"{board}:{year}:{number}")
                    number += 1

    ordered = sorted(questions.values(), key=lambda q: (q["subjectId"], q["chapterId"], q["type"], q.get("prompt", q.get("stem"))))
    counters = {}
    out = []
    for q in ordered:
        counters[q["subjectId"]] = counters.get(q["subjectId"], 0) + 1
        q = dict(q)
        q.pop("chapter")
        q["id"] = f"{q['subjectId']}-q{counters[q['subjectId']]:04d}"
        out.append({k: q[k] for k in (["id", "subjectId", "chapterId", "type"] +
                                      (["prompt", "options", "correctOptionId", "explanation"] if q["type"] == "MCQ" else ["stem", "parts"]) +
                                      ["appearances"])})
    return out


def main():
    questions = build()
    bundle = {
        "version": SEED_VERSION,
        "sample": True,
        "exams": EXAMS,
        "boards": [{"id": i, "name": n, "nameEn": e} for i, n, e in BOARDS],
        "notes": NOTES,
        "subjects": [
            {"id": sid, "exam": exam, "name": name, "nameEn": name_en, "code": code, "group": group,
             "chapters": [{"id": f"{sid}-{i + 1}", "number": i + 1, "name": c} for i, c in enumerate(chapters)]}
            for sid, exam, name, name_en, code, group, chapters in SUBJECTS
        ],
        "questions": questions,
    }
    OUTPUT.write_text(json.dumps(bundle, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    mcqs = sum(q["type"] == "MCQ" for q in questions)
    papers = {(q["subjectId"], a.rsplit(":", 1)[0]) for q in questions for a in q["appearances"]}
    repeated = sum(len(q["appearances"]) > 1 for q in questions)
    print(f"{len(questions)} questions ({mcqs} MCQ, {len(questions) - mcqs} CQ), {len(papers)} papers, "
          f"{repeated} repeated, {OUTPUT.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    main()
