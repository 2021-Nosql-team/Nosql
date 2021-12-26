user_num = set()
movie_num = set()
max_uid = 0
max_mid = 0
for line in open("model_train.csv"):
    data = line.split(',')
    uid = int(data[1])
    user_num.add(uid)
    if uid > max_uid:
        max_uid = uid
    mid = int(data[2])
    movie_num.add(mid)
    if mid > max_mid:
        max_mid = mid

print("uid_max = ", max_uid, " mid_max = ", max_mid, "\n")
print("user_num = ", len(user_num), " movie_num = ", len(movie_num))


#uid_max =  162541  mid_max =  209171  user_num =  162541  movie_num =  54001
#uid_max =  162541  mid_max =  209155