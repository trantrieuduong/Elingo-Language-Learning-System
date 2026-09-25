# import nltk
# nltk.download('wordnet')
# nltk.download('omw-1.4')

from nltk.corpus import wordnet

synsets = wordnet.synsets('dog')
print(synsets)
# Lấy định nghĩa của synset đầu tiên
print(synsets[0].definition())
# Ví dụ về cách dùng trong câu (examples)
print(synsets[0].examples())

# Lấy tên các từ đồng nghĩa trong synset
lemmas = synsets[0].lemmas()
print([l.name() for l in lemmas])

# Tìm từ trái nghĩa của từ 'good'
good_synset = wordnet.synset('good.a.01')
antonyms = good_synset.lemmas()[0].antonyms()
print(antonyms)
