import sys, os, glob
from keras.models import Sequential
from keras.layers import Convolution2D, MaxPooling2D
from keras.layers import Activation, Dropout, Flatten, Dense
# from keras.utils import np_utils
from keras import utils
from keras.models import load_model  # TensorFlow is required for Keras to work
from PIL import Image, ImageOps  # Install pillow instead of PIL
import numpy as np

from tensorflow.keras.applications.resnet50 import preprocess_input
from tensorflow.keras.preprocessing import image
import numpy as np
from PIL import Image
import matplotlib.pyplot as plt

import subprocess
import cv2

from pathlib import Path

from flask import Flask


app = Flask(__name__)
app_root = Path(app.root_path)  # Flask 앱의 루트 경로


def safe_path(file_name):
    return (app_root / file_name).as_posix()

weights_path = safe_path("best.pt")
source_path = safe_path("image.jpg")
detect_file_path = safe_path("./yolov5/detect.py")
output_image_path = safe_path("grabcut_image.jpg")

no_detected = 0


def remove_background_grabcut(image_path, output_path):
    # 이미지 읽기
    image = cv2.imread(image_path)
    height, width = image.shape[:2]

    # 이미지 중심 좌표
    center_x, center_y = width // 2, height // 2

    # 초기 마스크 생성
    mask = np.zeros(image.shape[:2], np.uint8)

    # 마스크를 위한 사전 설정
    # 확실한 배경
    border_margin = 10
    mask[:border_margin, :] = 0
    mask[-border_margin:, :] = 0
    mask[:, :border_margin] = 0
    mask[:, -border_margin:] = 0

    # 중심 영역을 전경으로 설정
    # margin_x = width // 3
    # margin_y = height정
    margin_x = width // 4
    margin_y = height // 2

    top_left_x = max(0, center_x - margin_x)
    top_left_y = max(0, center_y - margin_y)
    bottom_right_x = min(width, center_x + margin_x)
    bottom_right_y = min(height, center_y + margin_y)

    mask[top_left_y:bottom_right_y, top_left_x:bottom_right_x] = 3  # GC_PR_FGD

    # GrabCut에 필요한 임시 배열 생성
    bgd_model = np.zeros((1, 65), np.float64)
    fgd_model = np.zeros((1, 65), np.float64)

    # GrabCut 알고리즘 적용
    cv2.grabCut(image, mask, None, bgd_model, fgd_model, 5, cv2.GC_INIT_WITH_MASK)

    # 확실한 배경과 불확실한 배경을 0으로 설정, 확실한 전경과 불확실한 전경을 1로 설정
    mask2 = np.where((mask == 2) | (mask == 0), 0, 1).astype('uint8')

    # 원본 이미지와 마스크를 사용하여 결과 이미지 생성
    result = image * mask2[:, :, np.newaxis]

    # 결과 이미지 저장
    cv2.imwrite(output_path, result)


def crop_image(file_path):

    result_dir = app_root / 'yolov5' / 'runs' / 'detect' / 'exp'

    # 기존 결과 삭제
    if os.path.exists(result_dir):
        import shutil
        shutil.rmtree(result_dir)

    no_detected = 0
        
    cmd = f"python {detect_file_path} --weights {weights_path} --img 320 --conf 0.25 --source {source_path} --save-txt --save-conf --exist-ok --device cpu"

    result = subprocess.run(cmd, shell=True, capture_output=True, text=True) 

    # print(result)

    # 이미지 경로와 결과 파일 경로 설정
    txt_path = result_dir / 'labels' / f"{Path(file_path).stem}.txt"

    # 이미지 로드
    image = cv2.imread(file_path)
    height, width, _ = image.shape

    if txt_path.exists():
        with open(txt_path, 'r') as file:
            lines = file.readlines()
    else:
        no_detected = 1

        print(f"[First] No detection results file at {txt_path}")  # 결과 파일 경로 로그 추가

        # 객체 탐지 실패 시 GrabCut 수행 및 재탐지
        input_image_path = source_path
        remove_background_grabcut(input_image_path, output_image_path)

        cmd = f"python {detect_file_path} --weights {weights_path} --img 320 --conf 0.20 --source {output_image_path} --save-txt --save-conf --exist-ok --device cpu"
        result2 = subprocess.run(cmd, shell=True, capture_output=True, text=True)

        # print(result2)
    
    if no_detected == 1:
        txt_path_grab = result_dir / 'labels' / f"{Path(output_image_path).stem}.txt"

        if txt_path_grab.exists():
            with open(txt_path_grab, 'r') as file:
                lines = file.readlines()
        else:
            print(f"[Final] No detection results file at {txt_path_grab}")  # 결과 파일 경로 로그 추가
            return 0


    # 객체 데이터 파싱
    objects = []
    for line in lines:
        parts = line.strip().split()
        x_center, y_center, bbox_width, bbox_height = map(float, parts[1:5])
        x_center *= width
        y_center *= height
        bbox_width *= width
        bbox_height *= height
        x1 = int(x_center - bbox_width / 2)
        y1 = int(y_center - bbox_height / 2)
        x2 = int(x_center + bbox_width / 2)
        y2 = int(y_center + bbox_height / 2)
        objects.append((x1, y1, x2, y2))

    if not objects:
        return 0

    # 이미지 중앙과 가장 가까운 객체 찾기
    image_center = (width / 2, height / 2)
    closest_object = min(objects, key=lambda obj: (image_center[0] - (obj[0]+obj[2])/2) ** 2 + (image_center[1] - (obj[1]+obj[3])/2) ** 2)

    # 가장 가까운 객체를 사용하여 이미지 크롭
    x1, y1, x2, y2 = closest_object

    closest_object_area = (x2 - x1) * (y2 - y1)
    image_area = width * height

    if closest_object_area >= 0.9 * image_area:
        remove_background_grabcut(file_path, output_image_path)
        cmd = f"python {detect_file_path} --weights {weights_path} --img 320 --conf 0.20 --source {output_image_path} --save-txt --save-conf --exist-ok --device cpu"
        result3 = subprocess.run(cmd, shell=True, capture_output=True, text=True)

        txt_path_grabcut = result_dir / 'labels' / f"{Path(output_image_path).stem}.txt"

        if txt_path_grabcut.exists():
            with open(txt_path_grabcut, 'r') as file:
                lines = file.readlines()
        else:
            return 0

        objects = []
        for line in lines:
            parts = line.strip().split()
            x_center, y_center, bbox_width, bbox_height = map(float, parts[1:5])
            x_center *= width
            y_center *= height
            bbox_width *= width
            bbox_height *= height
            x1 = int(x_center - bbox_width / 2)
            y1 = int(y_center - bbox_height / 2)
            x2 = int(x_center + bbox_width / 2)
            y2 = int(y_center + bbox_height / 2)
            objects.append((x1, y1, x2, y2))

    if not objects:
        return 0
    

    closest_object = min(objects, key=lambda obj: (image_center[0] - (obj[0]+obj[2])/2) ** 2 + (image_center[1] - (obj[1]+obj[3])/2) ** 2)

    x1, y1, x2, y2 = closest_object

    cropped_image = image[y1:y2, x1:x2]

    # 크롭된 이미지 저장 또는 표시
    cv2.imwrite('./cropped_image.jpg', cropped_image)

    return len(objects)


def prepare_image(file_path, output_size=(384, 384)):
    # 이미지를 불러옵니다.
    img = cv2.imread(file_path)
    cropped_image = cv2.imread(file_path)

    # 이미지 사이즈 조절
    desired_size = 275
    old_size = img.shape[:2]
    if max(old_size) < desired_size:
        ratio = float(desired_size) / max(old_size)
        new_size = tuple([int(x * ratio) for x in old_size])
        img = cv2.resize(cropped_image, (new_size[1], new_size[0]))


    h, w, _ = img.shape
    # 새 이미지의 크기를 설정합니다.
    new_h, new_w = output_size

    # 배경 이미지를 생성합니다. 여기서는 흰색 배경을 사용합니다.
    result = np.full((new_h, new_w, 3), 255, dtype=np.uint8)

    # 새 이미지에서 원본 이미지가 위치할 시작점을 계산합니다.
    x_center = (new_w - w) // 2
    y_center = (new_h - h) // 2

    # 원본 이미지를 새 이미지의 중앙에 위치시킵니다.
    result[y_center:y_center+h, x_center:x_center+w] = img

    # 이미지를 RGB 형식으로 변환합니다.
    img_rgb = cv2.cvtColor(result, cv2.COLOR_BGR2RGB)
    # plt.imshow(img_rgb)
    # plt.show()

    # 이미지를 numpy 배열로 변환합니다.
    img_array = image.img_to_array(img_rgb)

    # 데이터 형식을 float32로 변경합니다.
    img_array = img_array.astype('float32')

    # 차원을 확장하여 모델 입력에 맞춥니다.
    img_array = np.expand_dims(img_array, axis=0)

    # 입력 이미지 배열을 전처리합니다.
    img_array = preprocess_input(img_array)

    return img_array


def classify_garbage(image_path):

    # Yolo 객체 탐지 및 Crop
    detected_cnt = crop_image(image_path)

    if detected_cnt == 0:
        predicted_class = 4
        garbage_class_name = "NONE"

        return predicted_class, garbage_class_name, detected_cnt


    cropped_image_path = './cropped_image.jpg'

    # 이미지 전처리
    prepared_image = prepare_image(cropped_image_path)

    # prepared_image_to_save = (prepared_image * 255).astype(np.uint8)[0]  # 차원 축소와 타입 변환
    # cv2.imwrite('./prepared_image.jpg', prepared_image_to_save)

    model = load_model("cnn(batch_size_256_lr_e-3).h5", compile=False)

    class_names = open("labels.txt", "r", encoding='UTF8').readlines()

    # 모델을 사용하여 예측
    predictions = model.predict(prepared_image)
    predicted_class = np.argmax(predictions, axis=1)[0]

    # print("predicted_class:", predicted_class)

    class_name = class_names[predicted_class]
    garbage_class_name = class_name[2:-1]

    # print(f"Predicted class: {class_labels[predicted_class]}")
    
    return predicted_class, garbage_class_name, detected_cnt


