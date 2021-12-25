# -*- coding: utf-8 -*-
import csv
import sys
import random
import math
from operator import itemgetter


random.seed(0)


class ItemBasedCF(object):
    ''' 基于物品的协同过滤 '''

    def __init__(self):
        self.trainset = {}
        self.testset = {}

        self.n_sim_movie = 20
        self.n_rec_movie = 10

        self.movie_sim_mat = {}
        self.movie_popular = {}
        self.movie_count = 0
        self.movie_slice_id = []
        self.movie_sim_list = {}
        self.recommend_list = {}

        print('Similar movie number = %d' % self.n_sim_movie, file=sys.stderr)
        print('Recommended movie number = %d' %
              self.n_rec_movie, file=sys.stderr)

    @staticmethod
    def loadfile(filename):
        ''' 加载文件 '''
        fp = open(filename, 'r')
        for i, line in enumerate(fp):
            yield line.strip('\r\n')
            if i % 100000 == 0:
                print('loading %s(%s)' % (filename, i), file=sys.stderr)
        fp.close()
        print('load %s succ' % filename, file=sys.stderr)

    def generate_dataset(self, filename, pivot=0.7):
        ''' 加载数据集'''
        trainset_len = 0
        testset_len = 0

        for line in self.loadfile(filename):
            if line == "":
                continue
            user, movie, rating, _ = line.split(',')
            self.trainset.setdefault(user, {})
            self.trainset[user][movie] = float(rating)
            trainset_len += 1

        print('train set = %s' % trainset_len, file=sys.stderr)


    def get_slice_movies(self, filename):
        """通过训练集计算电影流行度
        然后排序完给电影切分成不同的区间，并存储它们对应的流行度"""
        print('计算电影数量和流行度...', file=sys.stderr)

        for user, movies in self.trainset.items():
            for movie in movies:
                if movie not in self.movie_popular:
                    self.movie_popular[movie] = 0
                self.movie_popular[movie] += 1

        print('计算电影数量和流行度成功！', file=sys.stderr)
        self.movie_count = len(self.movie_popular)
        print("电影总数为%d" % self.movie_count, file=sys.stderr)

        # 给电影排序和存储流行度
        movie_slice = sorted(self.movie_popular.items(), key=lambda d: d[1], reverse=True)  # 排序
        movie_slice = movie_slice[int(0.6 * self.movie_count):]  # 切片范围

        # 存储文件
        fp = open(filename, "w", newline="")
        writer = csv.writer(fp)
        for item in movie_slice:
            movie_id = int(item[0])
            popular = int(item[1])
            writer.writerow([movie_id, popular])
        fp.close()

    def read_slice_movies(self, filename):
        """读取一个切片电影集，初始化他们的流行度"""

        for row in self.loadfile(filename):
            movieid, popular = row.split(',')
            self.movie_slice_id.append(movieid)
            self.movie_popular[movieid] = int(popular)

        print("读取切片电影列表")

    def get_slice_trainset(self, filename, trainset):
        """将训练集根据电影的切片结果也做一个区分"""
        fp_writer = open(filename, "w", newline="")
        writer = csv.writer(fp_writer)
        num = 0
        for line in self.loadfile(trainset):
            if line == "":
                continue
            user, movie, rating, _ = line.split(',')
            if movie not in self.movie_slice_id:
                continue
            writer.writerow([user, movie, rating, _])
            num += 1
        print("获得包含的电影的训练集 %d 个" % num)
        fp_writer.close()

    def calc_movie_sim(self):
        ''' 计算共现矩阵'''

        print('开始计算相关度矩阵', file=sys.stderr)

        num_user = 0
        for user, movies in self.trainset.items():
            num_user += 1
            for m1 in movies:
                for m2 in movies:
                    if m1 == m2:
                        continue
                    self.movie_sim_mat.setdefault(m1, {})
                    self.movie_sim_mat[m1].setdefault(m2, 0)
                    if abs(self.trainset[user][m1] - self.trainset[user][m2]) <= 2:
                        self.movie_sim_mat[m1][m2] += 1
            if num_user % 1000 == 0:
                print("有 %d 条记录跑完了训练集" % num_user)
        print('计算相关度矩阵', file=sys.stderr)


        print('计算共现矩阵', file=sys.stderr)
        simfactor_count = 0
        PRINT_STEP = 2000000

        for m1, related_movies in self.movie_sim_mat.items():
            for m2, count in related_movies.items():
                self.movie_sim_mat[m1][m2] = count / math.sqrt(self.movie_popular[m1] * self.movie_popular[m2])
                simfactor_count += 1
                if simfactor_count % PRINT_STEP == 0:
                    print('calculating movie similarity factor(%d)' %
                          simfactor_count, file=sys.stderr)

        print('计算共现矩阵成功',
              file=sys.stderr)

    def save_movies_sim(self, simFile):
        """存储共现矩阵"""
        fp = open(simFile, 'w', newline="")
        writer = csv.writer(fp)

        row_num = 0
        for m1 in self.movie_slice_id:
            row = [m1]
            item_num = 0
            if self.movie_sim_mat.get(m1, 0) == 0:
                continue

            # 排序后只取前五个相似电影，存储电影ID和相似度
            result = sorted(self.movie_sim_mat.get(m1).items(), key=lambda d: d[1], reverse=True)
            if len(result) >= 5:
                top5 = result[:5]
            else:
                top5 = result

            for m2 in top5:
                msg = str(m2[0]) + "_" + str(m2[1])
                row.append(msg)

            writer.writerow(row)
            row_num += 1
            print("有%d行成功存储进csv" % row_num, file=sys.stderr)
        fp.close()
        print("成功存储共现矩阵", file=sys.stderr)

    def recommend(self, user):
        ''' Find K similar movies and recommend N movies. '''
        K = self.n_sim_movie
        N = self.n_rec_movie
        rank = {}
        watched_movies = self.trainset[user]

        for movie, rating in watched_movies.items():
            for related_movie, similarity_factor in sorted(self.movie_sim_mat[movie].items(),
                                                           key=itemgetter(1), reverse=True)[:K]:
                if related_movie in watched_movies:
                    continue
                rank.setdefault(related_movie, 0)
                rank[related_movie] += similarity_factor * rating
        # return the N best movies
        return sorted(rank.items(), key=itemgetter(1), reverse=True)[:N]

    def get_sim_movies_list(self, filename):
        """加载相似电影列表"""
        for line in self.loadfile(filename):
            items = line.split(',')
            movie_id = items[0]
            self.movie_sim_mat.setdefault(movie_id, {})
            end_index = 6
            if len(items) <= 6:
                end_index = len(items)
            for item in items[1:end_index]:
                related_movie_id, relevance = item.split('_')
                self.movie_sim_mat[movie_id].setdefault(related_movie_id, 0)
                self.movie_sim_mat[movie_id][related_movie_id] = float(relevance)

    def generate_recommend(self):
        """获得推荐结果"""
        for user, watched_movies in self.trainset.items():
            self.recommend_list.setdefault(user, {})
            rank = {}

            # 遍历看过的电影，取每个电影的相似电影并打分
            for movie in watched_movies:
                sim_movies = self.movie_sim_mat.get(movie, {})
                for sim_movie, relevance in sim_movies.items():
                    pre_score = rank.get(sim_movie, 0)
                    # 计算评分 公式 = 用户评分 * 相似度
                    score = pre_score + self.trainset[user][movie] * relevance
                    rank.setdefault(sim_movie, 0)
                    rank[sim_movie] = score

            # 去掉用户看过的电影
            for movie in watched_movies:
                if movie in rank.keys():
                    rank.pop(movie)
            # 推荐列表进行排序 取前5个
            temp = sorted(rank.items(), key=itemgetter(1), reverse=True)
            if len(temp) >= 5:
                self.recommend_list[user] = temp[0:5]
            else:
                self.recommend_list[user] = temp

    def save_recommend(self, filename):
        """存储推荐列表"""
        fp = open(filename, 'w', newline="")
        writer = csv.writer(fp)
        row_num = 0
        for user, movies in self.recommend_list.items():
            row_num += 1
            row = [user]
            for movie in movies:
                row.append(movie[0])
            writer.writerow(row)
            row_num += 1
            print("%d 行存进了csv" % row_num, file=sys.stderr)

        fp.close()
        print("保存推荐列表成功", file=sys.stderr)


if __name__ == '__main__':
    itemcf = ItemBasedCF()
    itemcf.generate_dataset("train.csv")
    itemcf.get_sim_movies_list('top100.csv')
    itemcf.get_sim_movies_list('sim_fore20-60.csv')
    itemcf.get_sim_movies_list('sim_fore60-100.csv')
    itemcf.generate_recommend()
    itemcf.save_recommend('recommend.csv')
    # itemcf.read_slice_movies("60-100_movies.csv")
    # itemcf.get_slice_trainset("60-100_train.csv", "train.csv")

