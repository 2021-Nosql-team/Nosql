import tensorflow as tf
import csv
import ipykernel

# Training samples path, change to your local path
training_samples_file_path = tf.keras.utils.get_file("model_train.csv",
                                                     "file:///D:/programming/Nosql/model/test/model_train.csv")
# Test samples path, change to your local path
test_samples_file_path = tf.keras.utils.get_file("model_test.csv",
                                                 "file:///D:/programming/Nosql/model/test/model_test.csv")


# load sample as tf dataset
def get_dataset(file_path):
    dataset = tf.data.experimental.make_csv_dataset(
        file_path,
        batch_size=12,
        label_name='label',
        na_value="0",
        num_epochs=1,
        ignore_errors=True)
    return dataset


# split as test dataset and training dataset
train_dataset = get_dataset(training_samples_file_path)
test_dataset = get_dataset(test_samples_file_path)

# Config
# RECENT_MOVIES = 5  # userRatedMovie{1-5}
EMBEDDING_SIZE = 10

# define input for keras model
inputs = {
    'movieAvgRating': tf.keras.layers.Input(name='movieAvgRating', shape=(), dtype='float32'),
    # 'movieRatingStddev': tf.keras.layers.Input(name='movieRatingStddev', shape=(), dtype='float32'),
    # 'movieRatingCount': tf.keras.layers.Input(name='movieRatingCount', shape=(), dtype='int32'),
    'userAvgRating': tf.keras.layers.Input(name='userAvgRating', shape=(), dtype='float32'),
    # 'userRatingStddev': tf.keras.layers.Input(name='userRatingStddev', shape=(), dtype='float32'),
    # 'userRatingCount': tf.keras.layers.Input(name='userRatingCount', shape=(), dtype='int32'),
    'movieYear': tf.keras.layers.Input(name='movieYear', shape=(), dtype='int32'),
    'userPreferYear': tf.keras.layers.Input(name='userPreferYear', shape=(), dtype='int32'),

    'movieId': tf.keras.layers.Input(name='movieId', shape=(), dtype='int32'),
    'userId': tf.keras.layers.Input(name='userId', shape=(), dtype='int32'),
    # 'userRatedMovie1': tf.keras.layers.Input(name='userRatedMovie1', shape=(), dtype='int32'),
    # 'userRatedMovie2': tf.keras.layers.Input(name='userRatedMovie2', shape=(), dtype='int32'),
    # 'userRatedMovie3': tf.keras.layers.Input(name='userRatedMovie3', shape=(), dtype='int32'),
    # 'userRatedMovie4': tf.keras.layers.Input(name='userRatedMovie4', shape=(), dtype='int32'),
    # 'userRatedMovie5': tf.keras.layers.Input(name='userRatedMovie5', shape=(), dtype='int32'),

    'userRelTag1': tf.keras.layers.Input(name='userRelTag1', shape=(), dtype='int32'),
    'userRelTag2': tf.keras.layers.Input(name='userRelTag2', shape=(), dtype='int32'),
    'userRelTag3': tf.keras.layers.Input(name='userRelTag3', shape=(), dtype='int32'),
    'movieRelTag1': tf.keras.layers.Input(name='movieRelTag1', shape=(), dtype='int32'),
    'movieRelTag2': tf.keras.layers.Input(name='movieRelTag2', shape=(), dtype='int32'),
    'movieRelTag3': tf.keras.layers.Input(name='movieRelTag3', shape=(), dtype='int32'),
}

# movie id embedding feature
# movie_col = tf.feature_column.categorical_column_with_identity(key='movieId', num_buckets=1001)
# movie_emb_col = tf.feature_column.embedding_column(movie_col, EMBEDDING_SIZE)

# user id embedding feature
user_col = tf.feature_column.categorical_column_with_identity(key='userId', num_buckets=162542)
user_emb_col = tf.feature_column.embedding_column(user_col, EMBEDDING_SIZE)

# genre features vocabulary
# genre_vocab = ['Film-Noir', 'Action', 'Adventure', 'Horror', 'Romance', 'War', 'Comedy', 'Western', 'Documentary',
#                'Sci-Fi', 'Drama', 'Thriller',
#                'Crime', 'Fantasy', 'Animation', 'IMAX', 'Mystery', 'Children', 'Musical']
# user genre embedding feature
user_genre_col = tf.feature_column.categorical_column_with_identity(key="userRelTag1", num_buckets=1129)
user_genre_emb_col = tf.feature_column.embedding_column(user_genre_col, EMBEDDING_SIZE)
# item genre embedding feature
item_genre_col = tf.feature_column.categorical_column_with_identity(key="movieRelTag1", num_buckets=1129)
item_genre_emb_col = tf.feature_column.embedding_column(item_genre_col, EMBEDDING_SIZE)

'''
candidate_movie_col = [tf.feature_column.indicator_column(tf.feature_column.categorical_column_with_identity(key='movieId', num_buckets=1001,default_value=0))]
recent_rate_col = [
    tf.feature_column.indicator_column(tf.feature_column.categorical_column_with_identity(key='userRatedMovie1', num_buckets=1001,default_value=0)),
    tf.feature_column.indicator_column(tf.feature_column.categorical_column_with_identity(key='userRatedMovie2', num_buckets=1001,default_value=0)),
    tf.feature_column.indicator_column(tf.feature_column.categorical_column_with_identity(key='userRatedMovie3', num_buckets=1001,default_value=0)),
    tf.feature_column.indicator_column(tf.feature_column.categorical_column_with_identity(key='userRatedMovie4', num_buckets=1001,default_value=0)),
    tf.feature_column.indicator_column(tf.feature_column.categorical_column_with_identity(key='userRatedMovie5', num_buckets=1001,default_value=0)),
]
'''

candidate_movie_col = [tf.feature_column.numeric_column(key='movieId', default_value=0), ]

# recent_rate_col = [
#     tf.feature_column.numeric_column(key='userRatedMovie1', default_value=0),
#     tf.feature_column.numeric_column(key='userRatedMovie2', default_value=0),
#     tf.feature_column.numeric_column(key='userRatedMovie3', default_value=0),
#     tf.feature_column.numeric_column(key='userRatedMovie4', default_value=0),
#     tf.feature_column.numeric_column(key='userRatedMovie5', default_value=0),
# ]

# user profile
user_profile = [
    user_emb_col,
    user_genre_emb_col,
    # tf.feature_column.numeric_column('userRatingCount'),
    tf.feature_column.numeric_column('userAvgRating'),
    tf.feature_column.numeric_column('userPreferYear'),
    # tf.feature_column.numeric_column('userRatingStddev'),
]

# context features
context_features = [
    item_genre_emb_col,
    tf.feature_column.numeric_column('movieYear'),
    # tf.feature_column.numeric_column('movieRatingCount'),
    tf.feature_column.numeric_column('movieAvgRating'),
    # tf.feature_column.numeric_column('movieRatingStddev'),
]

candidate_layer = tf.keras.layers.DenseFeatures(candidate_movie_col)(inputs)
# user_behaviors_layer = tf.keras.layers.DenseFeatures(inputs)
user_profile_layer = tf.keras.layers.DenseFeatures(user_profile)(inputs)
context_features_layer = tf.keras.layers.DenseFeatures(context_features)(inputs)

# Activation Unit

movie_emb_layer = tf.keras.layers.Embedding(input_dim=209172, output_dim=EMBEDDING_SIZE, mask_zero=True)  # mask zero

# user_behaviors_emb_layer = movie_emb_layer(user_behaviors_layer)

candidate_emb_layer = movie_emb_layer(candidate_layer)
candidate_emb_layer = tf.squeeze(candidate_emb_layer, axis=1)

# activation_sub_layer = tf.keras.layers.Subtract()([candidate_emb_layer])  # element-wise sub
# activation_product_layer = tf.keras.layers.Multiply()([candidate_emb_layer])  # element-wise product
#
# activation_all = tf.keras.layers.concatenate([activation_sub_layer, candidate_emb_layer, activation_product_layer],
# axis=-1)

activation_unit = tf.keras.layers.Dense(32)(candidate_emb_layer)
activation_unit = tf.keras.layers.PReLU()(activation_unit)
activation_unit = tf.keras.layers.Dense(1, activation='sigmoid')(activation_unit)
activation_unit = tf.keras.layers.Flatten()(activation_unit)
activation_unit = tf.keras.layers.RepeatVector(EMBEDDING_SIZE)(activation_unit)
activation_unit = tf.keras.layers.Permute((2, 1))(activation_unit)
# activation_unit = tf.keras.layers.Multiply()([activation_unit])

# sum pooling
user_behaviors_pooled_layers = tf.keras.layers.Lambda(lambda x: tf.keras.backend.sum(x, axis=1))(activation_unit)

# fc layer
concat_layer = tf.keras.layers.concatenate([user_profile_layer, user_behaviors_pooled_layers,
                                            candidate_emb_layer, context_features_layer])
output_layer = tf.keras.layers.Dense(128)(concat_layer)
output_layer = tf.keras.layers.PReLU()(output_layer)
output_layer = tf.keras.layers.Dense(64)(output_layer)
output_layer = tf.keras.layers.PReLU()(output_layer)
output_layer = tf.keras.layers.Dense(1, activation='sigmoid')(output_layer)

model = tf.keras.Model(inputs, output_layer)
# compile the model, set loss function, optimizer and evaluation metrics
model.compile(
    loss='binary_crossentropy',
    optimizer='adam',
    metrics=['accuracy', tf.keras.metrics.AUC(curve='ROC'), tf.keras.metrics.AUC(curve='PR')])

# train the model
model.fit(train_dataset, epochs=5)

# evaluate the model
test_loss, test_accuracy, test_roc_auc, test_pr_auc = model.evaluate(test_dataset)
print(
    '\n\nTest Loss {}, Test Accuracy {}, Test ROC AUC {}, Test PR AUC {}'.format(test_loss, test_accuracy, test_roc_auc,
                                                                                 test_pr_auc))

# print some predict results
predictions = model.predict(test_dataset)
# for prediction, goodRating in zip(predictions[:12], list(test_dataset)[0][1][:12]):
#     print("Predicted good rating: {:.2%}".format(prediction[0]), " | Actual rating label: ",
#           ("Good Rating" if bool(goodRating) else "Bad Rating"))


def get_pre(pres):
    for p in pres:
        yield p[0]



with open("predictions.csv", "w", newline='') as csvfile:
    writer = csv.writer(csvfile)
    # 先写入columns_name
    writer.writerow(["userId", "movieId", "probability"])

    pre = get_pre(predictions)
    for i in list(test_dataset):
        for j in i:
            if isinstance(j, dict):
                for tf_uId, tf_mId in zip(j['userId'], j['movieId']):
                    writer.writerow([tf_uId.numpy().tolist(), tf_mId.numpy().tolist(), "{:.2%}".format(next(pre))])

    csvfile.close()
