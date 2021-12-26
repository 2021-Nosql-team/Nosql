import _thread
import requests
from bs4 import BeautifulSoup
import unicodedata
import logging
import csv
import queue

global s


class Model:
    def __init__(self):
        # 请求头
        self.headers = {
            'User-Agent': 'Mozilla/5.o (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) '
                          'Chrome/65.0.3325.162 Safari/537.36 '
        }
        # 存放每一步电影的id和imdb的id
        self.movie_dct = queue.Queue()
        # 存放已经处理完的movie id
        self.white_lst = []
        # 电影详情的初始url
        self.url = 'https://www.imdb.com/title/'
        self.movie_csv_path = './ml-25m/links.csv'
        # 海报的保存路径
        # self.poster_save_path = './poster'
        # 电影信息的保存文件
        self.info_save_path = './info.csv'
        # logging的配置，记录运行日志
        logging.basicConfig(filename="run.log", filemode="a+", format="%(asctime)s %(name)s:%(levelname)s:%(message)s",
                            datefmt="%Y-%m-%d %H:%M:%S", level=logging.INFO)
        # 表示当前处理的电影
        self.cur_movie_id = None
        self.cur_imdb_id = None
        self.get_movie_id()
        self.get_white_lst()

    def get_white_lst(self):
        """获取处理完的白名单"""
        with open('info.csv', encoding='utf8') as fb:
            for line in fb:
                line = line.strip()
                line = line.split(',')
                self.white_lst.append(line[0])

    def get_movie_id(self):
        """获取电影的id和imdb的id"""
        with open(self.movie_csv_path) as fb:
            fb.readline()
            for line in fb:
                line = line.strip()
                line = line.split(',')
                # 电影id 对应 imdbid
                self.movie_dct.put(line)

    # def update_white_lst(self, movie_id):
    #     """更新白名单"""
    #     with open('white_list.txt', 'a+') as fb:
    #         fb.write(movie_id + '\n')
    #
    # def update_black_lst(self, movie_id, msg=''):
    #     with open('black_list.txt', 'a+') as fb:
    #         # 写入movie id 和imdb id，并且加上错误原因
    #         # msg=1是URL失效，msg=2是电影没有海报
    #         fb.write(movie_id + ' ' + self.movie_dct[movie_id] + ' ' + msg + '\n')

    def get_url_response(self, url):
        """访问网页请求，返回response"""
        logging.info(f'get {url}')
        i = 0
        # 超时重传，最多5次
        while i < 5:
            try:
                response = requests.get(url, timeout=6)
                if response.status_code == 200:
                    logging.info(f'get {url} sucess')
                    # 正常获取，直接返回
                    return response
                # 如果状态码不对，获取失败，返回None，不再尝试
                logging.error(f'get {url} status_code error: {response.status_code} movie_id is {self.cur_movie_id}')
                return None
            except requests.RequestException:
                # 如果超时
                logging.error(f'get {url} error, try to restart {i + 1}')
                i += 1
        # 重试5次都失败，返回None
        return None

    def process_html(self, html, mid):
        """解析html，获取海报，电影信息"""
        soup = BeautifulSoup(html, 'html.parser')
        # 名字和发布日期 如：Toy Story (1995)
        name = soup.find('h1').get_text()
        # 去掉html的一些/x20等空白符
        name = unicodedata.normalize('NFKC', name)
        print(mid, " ", name)
        # poster_url = ''
        # try:
        #     # 海报的URL
        #     poster_url = soup.find(class_='poster').a.img['src']
        #     poster_re = self.get_url_response(poster_url)
        #     # 保存图片
        #     self.save_poster(self.cur_movie_id, poster_re.content)
        # except AttributeError as e:
        #     # 如果没有海报链接，那么在黑名单中更新它
        #     # msg=2表示没有海报链接
        #     self.update_black_lst(self.cur_movie_id, '2')

        # 电影的基本信息   1h 21min | Animation, Adventure, Comedy | 21 March 1996 (Germany)
        info = []
        # title下方的年份类型时间等
        tags = ""
        for inf in soup.find(
                class_='ipc-inline-list ipc-inline-list--show-dividers TitleBlockMetaData__MetaDataList-sc-12ein40-0 dxizHm baseAlt').find_all(
            class_='ipc-inline-list__item'):
            try:
                tags += inf.find('a').get_text().strip() + " "

            except AttributeError as e:
                tags += inf.get_text().strip() + " "

        info.append(tags)

        # 基本信息和详细发布时间 Animation, Adventure, Comedy
        try:
            if not soup.find(class_='ipc-chip-list GenresAndPlot__GenresChipList-cum89p-4 gtBDBL') is None:
                for tag in soup.find(class_='ipc-chip-list GenresAndPlot__GenresChipList-cum89p-4 gtBDBL').find_all(
                        class_='GenresAndPlot__GenreChip-cum89p-3 fzmeux ipc-chip ipc-chip--on-baseAlt'):
                    info.append(tag.find('span').get_text().strip())
            else:
                for tag in soup.find(class_='ipc-chip-list GenresAndPlot__OffsetChipList-cum89p-5 dMcpOf').find_all(
                        class_='GenresAndPlot__GenreChip-cum89p-3 fzmeux ipc-chip ipc-chip--on-baseAlt'):
                    info.append(tag.find('span').get_text().strip())
        except:
            info.append("")

        # 简介
        intro = soup.find(class_='GenresAndPlot__TextContainerBreakpointL-cum89p-1 gwuUFD').get_text().strip()
        intro = unicodedata.normalize('NFKC', intro)
        info.append(intro)
        # 卡司。D W S C，分别表示 导演，编剧，明星，导演
        case_dict = {'D': [], 'W': [], 'S': [], 'C': []}
        try:
            for names in soup.find(
                    class_='ipc-metadata-list ipc-metadata-list--dividers-all title-pc-list ipc-metadata-list--baseAlt').find_all(
                class_='ipc-metadata-list__item'):
                ch = names.find(class_='ipc-metadata-list-item__label').get_text().strip()[0]
                for n in names.find(
                        class_='ipc-inline-list ipc-inline-list--show-dividers ipc-inline-list--inline ipc-metadata-list-item__list-content baseAlt').find_all(
                    class_='ipc-metadata-list-item__list-content-item'):
                    case_dict[ch].append(n.get_text().strip())
        except:
            pass

        # 有时候导演名会用Creator代替
        if 'C' in case_dict.keys():
            case_dict['D'].extend(case_dict['C'])
        # id，电影名称，时长，类型，发行时间，简介，导演，编剧，演员
        detail = [mid, name, info[0], '|'.join(info[1:-1]), info[-1],
                  '|'.join(case_dict['D']), '|'.join(case_dict['W']), '|'.join(case_dict['S'])]
        self.save_info(detail)

    # def save_poster(self, movie_id, content):
    #     with open(f'{self.poster_save_path}/{movie_id}.jpg', 'wb') as fb:
    #         fb.write(content)

    def save_info(self, detail):
        # 存储到CSV文件中
        with open(f'{self.info_save_path}', 'a+', encoding='utf-8', newline='') as fb:
            writer = csv.writer(fb)
            writer.writerow(detail)

    def run(self):
        # 开始爬取信息
        # 先读入文件
        # self.get_white_lst()
        while not self.movie_dct.empty():

            line = self.movie_dct.get()
            self.cur_movie_id = line[0]
            self.cur_imdb_id = line[1]
            if line[0] in self.white_lst:
                continue
            # 休眠，防止被封IP，大概3秒处理完一部电影的信息，如果注释掉，会减少大约2.5小时的运行时间
            # IMDB好像没有反爬机制，可以放心的注释掉
            # time.sleep(1)
            response = self.get_url_response(self.url + 'tt' + self.cur_imdb_id)
            # 找不到电影详情页的url，或者超时，则仅仅保留id，之后再用另一个脚本处理
            if response is None:
                # self.save_info([self.cur_movie_id, '' * 9])
                # 仍然更新白名单，避免重复爬取这些失败的电影
                # self.update_white_lst(self.cur_movie_id)
                # 更新黑名单，爬完之后用另一个脚本再处理
                # self.update_black_lst(self.cur_movie_id, '1')
                continue
            # 处理电影详情信息
            self.process_html(response.content, line[0])
            # 处理完成，增加movie id到白名单中
            # self.update_white_lst(self.cur_movie_id)
            logging.info(f'process movie {self.cur_movie_id} success')


def init_model():
    s.run()


if __name__ == '__main__':
    s = Model()

    try:
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
        _thread.start_new_thread(init_model, ())
    except:
        print("ERROR")

    while 1:
        pass
