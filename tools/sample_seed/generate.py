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
    ("ssc-math", "SSC", "গণিত", "General Mathematics", "109", "COMMON",
     ["বাস্তব সংখ্যা", "সেট ও ফাংশন", "বীজগাণিতিক রাশি"]),
    ("ssc-bgs", "SSC", "বাংলাদেশ ও বিশ্বপরিচয়", "Bangladesh and Global Studies", "150", "COMMON",
     ["মুক্তিযুদ্ধ ও বাংলাদেশের অভ্যুদয়", "বাংলাদেশের সংবিধান", "বাংলাদেশ ও আন্তর্জাতিক সহযোগিতা"]),
    ("ssc-ict", "SSC", "তথ্য ও যোগাযোগ প্রযুক্তি", "ICT", "154", "COMMON",
     ["তথ্য ও যোগাযোগ প্রযুক্তি ও আমাদের বাংলাদেশ", "কম্পিউটার ও কম্পিউটার ব্যবহারকারীর নিরাপত্তা", "আমার লেখালেখি ও হিসাব"]),
    ("ssc-physics", "SSC", "পদার্থবিজ্ঞান", "Physics", "136", "SCIENCE",
     ["ভৌত রাশি এবং পরিমাপ", "গতি", "বল"]),
    ("ssc-chemistry", "SSC", "রসায়ন", "Chemistry", "137", "SCIENCE",
     ["রসায়নের ধারণা", "পদার্থের অবস্থা", "পদার্থের গঠন"]),
    ("ssc-biology", "SSC", "জীববিজ্ঞান", "Biology", "138", "SCIENCE",
     ["জীবন পাঠ", "জীবকোষ ও টিস্যু", "কোষ বিভাজন"]),
    ("ssc-accounting", "SSC", "হিসাববিজ্ঞান", "Accounting", "146", "BUSINESS",
     ["হিসাববিজ্ঞান পরিচিতি", "লেনদেন", "দুতরফা দাখিলা পদ্ধতি"]),
    ("hsc-ict", "HSC", "তথ্য ও যোগাযোগ প্রযুক্তি", "ICT", "275", "COMMON",
     ["বিশ্ব ও বাংলাদেশ প্রেক্ষিত", "কমিউনিকেশন সিস্টেমস ও নেটওয়ার্কিং", "সংখ্যা পদ্ধতি ও ডিজিটাল ডিভাইস"]),
    ("hsc-physics-1", "HSC", "পদার্থবিজ্ঞান ১ম পত্র", "Physics 1st Paper", "174", "SCIENCE",
     ["ভৌতজগৎ ও পরিমাপ", "ভেক্টর", "গতিবিদ্যা"]),
    ("hsc-chemistry-1", "HSC", "রসায়ন ১ম পত্র", "Chemistry 1st Paper", "176", "SCIENCE",
     ["ল্যাবরেটরির নিরাপদ ব্যবহার", "গুণগত রসায়ন", "মৌলের পর্যায়বৃত্ত ধর্ম ও রাসায়নিক বন্ধন"]),
    ("hsc-biology-1", "HSC", "জীববিজ্ঞান ১ম পত্র", "Biology 1st Paper", "178", "SCIENCE",
     ["কোষ ও এর গঠন", "কোষ বিভাজন", "কোষ রসায়ন"]),
    ("hsc-highermath-1", "HSC", "উচ্চতর গণিত ১ম পত্র", "Higher Mathematics 1st Paper", "265", "SCIENCE",
     ["ম্যাট্রিক্স ও নির্ণায়ক", "ভেক্টর", "সরলরেখা"]),
    ("hsc-accounting-1", "HSC", "হিসাববিজ্ঞান ১ম পত্র", "Accounting 1st Paper", "253", "BUSINESS",
     ["হিসাববিজ্ঞান পরিচিতি", "হিসাবের বইসমূহ", "ব্যাংক সমন্বয় বিবরণী"]),
    ("hsc-economics-1", "HSC", "অর্থনীতি ১ম পত্র", "Economics 1st Paper", "109", "HUMANITIES",
     ["মৌলিক অর্থনৈতিক সমস্যা", "ভোক্তা ও উৎপাদকের আচরণ", "চাহিদা ও যোগান"]),
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


GENERATORS = {
    "ssc-math": (g_ssc_math, g_ssc_math_fixed, cq_ssc_math),
    "ssc-bgs": (g_ssc_bgs, g_ssc_bgs, cq_ssc_bgs),
    "ssc-ict": (g_ssc_ict, g_ssc_ict_fixed, cq_ssc_ict),
    "ssc-physics": (g_ssc_physics, g_ssc_physics_fixed, cq_ssc_physics),
    "ssc-chemistry": (g_ssc_chemistry, g_ssc_chemistry, cq_ssc_chemistry),
    "ssc-biology": (g_ssc_biology, g_ssc_biology_fixed, cq_ssc_biology),
    "ssc-accounting": (g_ssc_accounting, g_ssc_accounting, cq_ssc_accounting),
    "hsc-ict": (g_hsc_ict, g_hsc_ict, cq_hsc_ict),
    "hsc-physics-1": (g_hsc_physics, g_hsc_physics, cq_hsc_physics),
    "hsc-chemistry-1": (g_hsc_chemistry, g_hsc_chemistry, cq_hsc_chemistry),
    "hsc-biology-1": (g_hsc_biology, g_hsc_biology, cq_hsc_biology),
    "hsc-highermath-1": (g_hsc_highermath, g_hsc_highermath, cq_hsc_highermath),
    "hsc-accounting-1": (g_hsc_accounting, g_hsc_accounting, cq_hsc_accounting),
    "hsc-economics-1": (g_hsc_economics, g_hsc_economics, cq_hsc_economics),
}


# ----------------------------------------------------------------------------- assembly

def paper_exists(exam, group, board, year):
    if exam == "HSC" and year == 2020:
        return False
    if year == 2021 and group == "COMMON":
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
                    key = content_key(q)
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
