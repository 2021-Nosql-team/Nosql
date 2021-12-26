import tensorflow as tf
import csv

model = tf.keras.models.load_model('D:/programming/Nosql/model/test/model.h5')

test_samples_file_path = tf.keras.utils.get_file("D:/programming/Nosql/model/test/model_temp.csv",
                                                 "file:///D:/programming/Nosql/model/test/model_temp.csv")


def get_dataset(file_path):
    dataset = tf.data.experimental.make_csv_dataset(
        file_path,
        batch_size=12,
        label_name='label',
        na_value="0",
        num_epochs=1,
        ignore_errors=True)
    return dataset


test_dataset = get_dataset(test_samples_file_path)

# print("predict start")

# print some predict results
predictions = model.predict(test_dataset)


# print("predict end")

# for prediction, goodRating in zip(predictions[:12], list(test_dataset)[0][1][:12]):
#     print("Predicted good rating: {:.2%}".format(prediction[0]), " | Actual rating label: ",
#           ("Good Rating" if bool(goodRating) else "Bad Rating"))


def get_pre(pres):
    for p in pres:
        yield p[0]


print("start writing")

with open("predictions_100.csv", "w", newline='') as csvfile:
    writer = csv.writer(csvfile)
    # 先写入columns_name
    writer.writerow(["userId", "movieId", "probability"])

    pre = get_pre(predictions)

    print("1")

    for i in list(test_dataset):
        print("2")

        for j in i:
            if isinstance(j, dict):
                for tf_uId, tf_mId in zip(j['userId'], j['movieId']):
                    writer.writerow([tf_uId.numpy().tolist(), tf_mId.numpy().tolist(), "{:.2%}".format(next(pre))])

    csvfile.close()
