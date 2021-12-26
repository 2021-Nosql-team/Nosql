import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'  # 不显示等级2以下的提示信息
import tensorflow as tf


model = tf.keras.models.load_model('D:/programming/Nosql/model/test/model.h5')


test_samples_file_path = tf.keras.utils.get_file("D:/programming/Nosql/model/test/model_toPrediction.csv",
                                                 "file:///D:/programming/Nosql/model/test/model_toPrediction.csv")

def get_dataset(file_path):
    dataset = tf.data.experimental.make_csv_dataset(
        file_path,
        batch_size=12,
        label_name='label',
        num_epochs=1,
        ignore_errors=True)
    return dataset


test_dataset = get_dataset(test_samples_file_path)

# print some predict results
predictions = model.predict(test_dataset)


def get_pre(pres):
    for p in pres:
        yield p[0]


rows = []

pre = get_pre(predictions)
for i in list(test_dataset):
    for j in i:
        if isinstance(j, dict):
            for tf_uId, tf_mId in zip(j['userId'], j['movieId']):
                rows.append([tf_uId.numpy().tolist(), tf_mId.numpy().tolist(), "{:.2%}".format(next(pre))])

sortList = sorted(rows, key=lambda row: row[2], reverse=True)

result = []
for i in range(5):
    result.append(sortList[i][1:3])

print(result)
