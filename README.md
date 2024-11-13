<center>
    <img src="frontend/assets/images/logo.png" width=30%>
    <br>
    <span style="font-size:200%">더러워진 지구를 구하기 위한 플로깅 용사들의 모험</span>  
</center>

<br>
<br>

# 목차

- [팀원 소개](#참여-인원)<br>
- [개요](#개요)<br>
- [주요 기능](#주요-기능)<br>
- [서비스 화면](#서비스-화면)<br>
- [시스템 아키텍처](#시스템-아키텍처)<br>
- [주요 기술](#주요-기술)<br>
- [파일 구조](#파일-구조)<br>
<!-- - [프로젝트 산출물](#프로젝트-산출물)<br> -->

<br>
<br>

# 참여 인원

총 6명
<!-- |    전규훈    | 김민욱 | 강성권 |    김혁일    |   최시원    | 박미성 |
| :----------: | :----: | :----: | :----------: | :---------: | :----: |
| Iot (Leader) | Front  | Front  | Back, Server | Back,Server |   AI   | -->

<br>
<br>

# 개요

    - 환경에 대한 관심이 높아지면서 그와 더불어 플로깅에 대한 관심도 지속적으로 증가하고 있다.
    - 하지만 현재 플로깅은 환경의 날과 같은 특별한 날에만 실시하는 일종의 행사 같은 느낌이 있다.
    - 이러한 단발적인 플로깅 행사들을 넘어 사람들의 지속적인 플로깅 참여를 독려하기 위한 어플리케이션

<br>
<br>

# 주요 기능

### 플로깅을 진행하면서 집게에 장착한 Iot 기기를 통한 쓰레기 종류 판별

    - 어플리케이션과 집게에 장착한 Iot 기기와 블루투스 연결
    - 플로깅을 진행하면서 Iot 기기로 주운 쓰레기 사진을 찍고 AI로 쓰레기 종류 판별

### 플로깅 진행 후 쓰레기 종류 판별을 기반으로 한 펫 키우기 시스템

    - 20여 종의 멸종위기 동물 캐릭터(펫) 키우기 기능
    - 판별한 쓰레기 종류에 따라 펫에게 경험치 혹은 유저에게 재화 제공
    - 주간 퀘스트, 업적을 통한 펫, 재화 획득 기능
    - 획득한 재화를 기반으로 한 펫 뽑기 기능
    - 펫을 키우며 획득한 경험치를 기반으로 한 유저별 랭킹정보 조회 기능
    - 플로깅을 진행했던 날짜별 히스토리 정보 제공

### 내 주변 쓰레기통 위치 정보, 지역별 쓰레기 현황 정보 제공

    - 현재 위치 기준 주변 쓰레기통 데이터에 따라 쓰레기통 위치 정보 제공(현재는 광주 광산구 한정)
    - 유저가 플로깅하면서 주웠던 쓰레기 정보를 저장하고, 그 정보에 따른 지역별 쓰레기 현황 제공

### 전국 단위의 플로깅 정보 제공

    - 기존의 단발적이고 지역 한정적인 플로깅 행사 정보를 한 데 모아 볼 수 있는 기능
    - 혼자하는 플로깅을 넘어 함께 참여할 수 있는 플로깅 참여 독려

<br>
<br>

# 서비스 화면

## 인트로, 튜토리얼

| ![인트로](images/인트로.gif) | ![튜토리얼](images/튜토리얼.gif) |
| :--------------------------: | :------------------------------: |
|            인트로            |             튜토리얼             |

## 메인화면

| ![메인화면](images/메인몬스터.gif) |
| :--------------------------------: |
|               인트로               |

## 주간 퀘스트, 업적

| ![주간 퀘스트](images/퀘스트.gif) | ![업적](images/업적.gif) |
| :-------------------------------: | :----------------------: |
|            주간 퀘스트            |           업적           |

## 랭킹

| ![랭킹](images/랭킹.gif) |
| :----------------------: |
|           랭킹           |

## 히스토리

| ![히스토리](images/히스토리.gif) |
| :------------------------------: |
|             히스토리             |

## 캠페인

| ![캠페인](images/캠페인.gif) |
| :--------------------------: |
|            캠페인            |

## 펫

| ![펫 획득](images/상자오픈.gif) | ![펫 구출](images/구조.gif) |
| :-----------------------------: | :-------------------------: |
|             펫 획득             |           펫 구출           |

## 플로깅

| ![플로깅 준비](images/플로깅준비.gif) | ![플로깅 진행](images/플로깅진행.gif) |
| :-----------------------------------: | :-----------------------------------: |
|              플로깅 준비              |              플로깅 진행              |

## 데이터 차단, 자동 로그인

| ![데이터 차단, 자동 로그인](images/자동로그인.gif) |
| :------------------------------------------------: |
|              데이터 차단, 자동 로그인              |

<br>
<br>

# 시스템 아키텍처

![아키텍처](images/Web_App_Reference_Architecture.png)

# 주요 기술

#### 1. FE

- Android Studio

  - Hedgehog 2023.1.1 Patch 2

- Visual Studio

- Flutter 3.19.1

#### 2. BE

- OpenJDK 17

- Spring Boot 3.2.4
  - Project : Gradle - Groovy
- Spring Data JPA
- Spring Security
- Spring Validation
- Redis (master-slave)
- JWT
- Hibernate Spatial
- JTS
- swagger

- TEST
  - Apache JMeter

#### 3. DB

- MYSQL 8.0.35

#### 4. INFRA

- Docker : 25.0.2

- DockerHub

- Jenkins : 2.445

- nginx : 1.18(Ubuntu)

- SSL

#### 5. AI

- 학습 환경 : Colab, Jupyter Notebook, Python 3.8.10

- Library : Keras 2.13.1, Tensorflow 2.13.1, Numpy 1.24.3, Torch 1.8.0, OpenCV 4.9.0.80

- Model : ResNet50, YoloV5

- 데이터셋 : AI Hub, Roboflow

- 모델 서빙 서버 : Flask 3.0.3

#### IoT

- 개발 환경 : Arduino IDE, Visual Studio Code

- MCU : ESP32-CAM

![회로도](images/circuit.png)

#### 6. 협업툴

- 형상 관리 : Gitlab

- 이슈 관리 : Jira

- 커뮤니케이션 : Mattermost, Notion, Miro

- 디자인 : Figma, Canva

<br>
<br>

# 파일 구조

#### Backend

```
BackEnd\src\main\java\com\c206\backend
├─ 📂domain
│  ├─ 📂achievement
│  │  ├─ 📂controller
│  │  ├─ 📂dto
│  │  │  └─ 📂response
│  │  ├─ 📂entity
│  │  │  └─ 📂enums
│  │  ├─ 📂exception
│  │  ├─ 📂repository
│  │  └─ 📂service
│  ├─ 📂history
│  │  ├─ 📂controller
│  │  ├─ 📂dto
│  │  │  └─ 📂response
│  │  ├─ 📂exception
│  │  └─ 📂service
│  ├─ 📂member
│  │  ├─ 📂controller
│  │  ├─ 📂dto
│  │  │  ├─ 📂request
│  │  │  └─ 📂response
│  │  ├─ 📂entity
│  │  ├─ 📂exception
│  │  │  ├─ 📂member
│  │  │  └─ 📂redis
│  │  ├─ 📂repository
│  │  └─ 📂service
│  ├─ 📂notice
│  │  ├─ 📂controller
│  │  ├─ 📂dto
│  │  │  └─ 📂response
│  │  ├─ 📂entity
│  │  ├─ 📂exception
│  │  ├─ 📂repository
│  │  └─ 📂service
│  ├─ 📂pet
│  │  ├─ 📂controller
│  │  ├─ 📂dto
│  │  │  └─ 📂response
│  │  ├─ 📂entity
│  │  │  └─ 📂enums
│  │  ├─ 📂exception
│  │  ├─ 📂repository
│  │  └─ 📂service
│  ├─ 📂plogging
│  │  ├─ 📂controller
│  │  ├─ 📂dto
│  │  │  ├─ 📂request
│  │  │  └─ 📂response
│  │  ├─ 📂entity
│  │  │  └─ 📂enums
│  │  ├─ 📂exception
│  │  ├─ 📂repository
│  │  └─ 📂service
│  │      └─ 📂Impl
│  ├─ 📂quest
│  │  ├─ 📂controller
│  │  ├─ 📂dto
│  │  │  └─ 📂response
│  │  ├─ 📂entity
│  │  ├─ 📂exception
│  │  ├─ 📂repository
│  │  └─ 📂service
│  └─ 📂ranking
│      ├─ 📂controller
│      ├─ 📂dto
│      │  └─ 📂response
│      └─ 📂service
└─ 📂global
    ├─ 📂common
    │  ├─ 📂dto
    │  └─ 📂entity
    ├─ 📂config
    ├─ 📂exception
    └─ 📂jwt
```

#### Frontend

```
frontend\lib
├─ 📂core
│  └─ 📂theme
│      ├─ 📂constant
│      └─ 📂custom
├─ 📂models
├─ 📂provider
├─ 📂router
└─ 📂screens
    ├─ 📂campaign
    │  ├─ 📂component
    │  └─ 📂dialog
    ├─ 📂component
    │  ├─ 📂clearmonster
    │  └─ 📂topbar
    │      └─ 📂dialog
    ├─ 📂history
    │  ├─ 📂component
    │  └─ 📂dialog
    ├─ 📂intro
    │  └─ 📂dialog
    ├─ 📂main
    │  ├─ 📂component
    │  ├─ 📂dialog
    │  ├─ 📂openbox
    │  └─ 📂partner
    ├─ 📂member
    │  └─ 📂component
    ├─ 📂plogging
    │  ├─ 📂finishplogging
    │  │  └─ 📂component
    │  ├─ 📂progressplogging
    │  │  ├─ 📂component
    │  │  └─ 📂dialog
    │  └─ 📂readyplogging
    │      ├─ 📂component
    │      └─ 📂dialog
    ├─ 📂profile
    │  ├─ 📂component
    │  └─ 📂dialog
    ├─ 📂ranking
    │  └─ 📂component
    ├─ 📂rescue
    │  └─ 📂dialog
    └─ 📂tutorial
```

<br>
<br>

<!-- # 프로젝트 산출물

- [요구사항 명세서](https://www.notion.so/SSAFY-C206-a9bd69e29d404bfba469baaba88bcfbc)
- [API 명세서](https://www.notion.so/SSAFY-C206-a9bd69e29d404bfba469baaba88bcfbc)
- [ERD](https://www.erdcloud.com/team/ijemLkdQX5afChLjg)
- [와이어프레임(피그마)](https://www.figma.com/files/project/221823514/Team-project?fuid=1324572789228621233) -->
