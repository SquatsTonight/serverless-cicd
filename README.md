# Serverless CI/CD -- 基于 Serverless-Devs 以及 Github Actions 的实现

## 前言

首先介绍下在本文出现的几个比较重要的概念：

> **函数计算（Function Compute）**: 函数计算是一个事件驱动的服务，通过函数计算，用户无需管理服务器等运行情况，只需编写代码并上传。函数计算准备计算资源，并以弹性伸缩的方式运行用户代码，而用户只需根据实际代码运行所消耗的资源进行付费。更多信息[参考](https://help.aliyun.com/product/50980.html)。

> **Serverless Devs**: Serverless Devs 是一个开源开放的 Serverless 开发者平台，致力于为开发者提供强大的工具链体系。通过该平台，开发者可以一键体验多云 Serverless 产品，极速部署 Serverless 项目。更多信息[参考](https://github.com/Serverless-Devs/Serverless-Devs/blob/master/readme_zh.md)。

> **CI/CD**: CI/CD 是一种通过在应用开发阶段引入自动化来频繁向客户交付应用的方法。CI/CD 的核心概念是持续集成、持续交付和持续部署。

## 目标

本文以 Nodejs(解释型) 和 Java(编译型) 的函数计算项目为例实现其 CI/CD 流程：

    1. Github Pull Request 触发 CI/CD
    2. 构建项目
    3. 执行测试
    4. 部署发布

## 工作流程图
![Alt text](https://img.alicdn.com/tfs/TB1Y1c73pT7gK0jSZFpXXaTkpXa-1032-730.png 'Optional Title')

对于编译型 Runtime，例如 Java，首先需要构建出对应的交付物，构建的过程可以理解为，去函数代码目录查找特定的 manifest 文件（清单文件），然后根据这些 manifest 文件进行特定的构造、依赖下载、编译等操作。CI/CD 流程中需要构建两次，一次构建目标为测试环境，一次构建目标为生产环境。‘

对于解释型 Runtime，例如 Nodejs，构建的的过程可以理解为安装项目依赖，在此基础上执行项目测试以及部署发布。

## 项目介绍

项目主要包含一个函数计算 Nodejs10 http 触发器函数以及 Java8 事件函数，项目目录结构如下：

```text
serverless-cicd
├── .github
│   └── workflows
│       └── cicd.yml
├── .gitignore
├── README.md
└── src
    ├── fc-java8-event
    │   ├── .npmignore
    │   ├── e2e
    │   │   ├── index.e2e-test.js
    │   │   ├── jest.config.e2e.js
    │   │   ├── package-lock.json
    │   │   └── package.json
    │   ├── pom.xml
    │   └── src
    │       ├── main
    │       │   └── java
    │       │       └── example
    │       │           ├── HelloFC.java
    │       │           ├── SimpleRequest.java
    │       │           └── SimpleResponse.java
    │       └── test
    │           └── java
    │               └── example
    │                   └── HelloFCTest.java
    ├── fc-nodejs10-http
    │   ├── index.e2e-test.js
    │   ├── index.js
    │   ├── index.test.js
    │   ├── jest.config.e2e.js
    │   ├── package-lock.json
    │   └── package.json
    └── template.yml
```

部分文件/目录介绍：

    * cicd.yml - Github Actions 配置文件
    * template.yml - Serverless Devs 配置文件
    * fc-nodejs10-http - 函数计算 Nodejs10 http 触发器函数源码以及测试代码目录
    * fc-java8-event - 函数计算 Java8 事件函数源码以及测试代码目录

Serverless Devs 配置文件格式为：

```yaml
ProjectName:
  Component: the Component name.
  Provider: cloud vendor
  Properties: parameters defined by the component
```

配置文件中可以定义全局变量，并使用环境变量，例如：

```yaml
Global:
  ServiceDefinition:
    Name: ${Env(SERVICE_NAME)}
    Description: This is a serivce

NodejsProject:
  Component: fc
  Provider: ${Env(PROVIDER)}
  Properties:
    Region: ${Env(REGION)}
    Service: ${Global.ServiceDefinition}
    Function:
      Name: ${Env(NODEJS_FUNCTION_NAME)}
      Description: This is a http trigger function of Nodejs10
      CodeUri: ./fc-nodejs10-http
      Handler: index.handler
      MemorySize: 128
      Runtime: nodejs10
      Timeout: 60
      Triggers:
        - Name: TriggerNameHttp
          Type: HTTP
          Parameters:
            AuthType: ANONYMOUS
            Methods:
              - GET
```

上述配置中定义了全局变量 `ServiceDefinition` ，以 `${Global.ServiceDefinition}` 方式获取，此外， `${Env(NODEJS_FUNCTION_NAME)}` 表示获取环境变量 `NODEJS_FUNCTION_NAME`。

使用 Serverless Devs 时需要指定云厂商名称和组件名称，并给出相关组件的属性，更多信息请参考[这里](https://github.com/Serverless-Devs/docs/blob/master/docs/en/tool/yaml_format.md)。本文以[阿里巴巴函数计算组件](https://github.com/Serverless-Devs-Awesome/fc-alibaba-component/)进行开发使用。

Github Actions 无需用户进行配置，只需要在您的 Github 项目下创建 .github/workflow/***.yml 文件即可进行使用。

## Nodejs 项目的 CI/CD

该案例是基于函数计算 http 触发器实现的函数，访问函数会返回指定时区的当前时间。下面基于 [cicd.yml](.github/workflows/cicd.yml) 文件的内容详细介绍 Nodejs 案例的 CI/CD 流程。

### 初始化开发环境

初始化开发环境分为初始化 Serverless Devs 工具以及搭建开发语言环境两部分。

#### **Serverless Devs**

初始化 Serverless Devs 的 GitHub Actions 模版可参考[这里](https://github.com/Serverless-Devs/serverless-devs-initialization-action)，本案例使用该模版安装并配置 Serverless Devs。

```yaml
- name: Initializing Serverless-Devs
  uses: Serverless-Devs/serverless-devs-initialization-action@main
  with:
    provider: ${{ env.PROVIDER }}
    AccessKeyID: ${{ secrets.ALIYUN_ACCESS_KEY_ID }}
    AccessKeySecret: ${{ secrets.ALIYUN_ACCESS_KEY_SECRET }}
    AccountID: ${{ secrets.ALIYUN_ACCOUNT_ID }}
```

上述 `with` 中包含 serverless-devs-initialization-action 的输入，不同的云厂商需要不同的输入参数，其中 Credentials 信息需要存储在 [Github Secret](https://docs.github.com/en/free-pro-team@latest/actions/reference/encrypted-secrets) 中。

#### **Nodejs10**

本案例使用 `actions/setup-node` 的 Github Actions 模版初始化开发语言环境。

```yaml
- name: Setup nodejs10
  uses: actions/setup-node@v1
  with:
    node-version: '10'
```

### 构建项目

阿里巴巴函数计算构建的 Github Actions 模版可参考[这里](https://github.com/git-qfzhang/alibaba-fc-build-action/)，本案例使用该模版构建项目。

```yaml
- name: Building
  uses: git-qfzhang/alibaba-fc-build-action@main
  env:
    PROVIDER: ${{ env.PROVIDER }}
    REGION: ${{ env.REGION }}
    SERVICE_NAME: ${{ env.PROD_SERVICE_NAME }}
    NODEJS_FUNCTION_NAME: ${{ env.PROD_NODEJS_FUNCTION_NAME }}
  with:
    working_directory: ${{ env.FC_CODE_URI }}
    projects: ${{ env.NODEJS_PROJECT_NAME }}
```

对于解释型语言，构建的过程只需安装依赖，因此在 CI/CD 流程中只需构建一次即可，后续步骤可以复用这些依赖。

### 执行测试

#### 单元测试

本案例采用 jest 测试框架进行测试，主要分为三步：

    1. mock 依赖的值或者参数
    2. 调用测试函数
    3. 断言返回结果和被调用的参数

测试内容详情请见[index.test.js](src/fc-nodejs10-http/index.test.js)

#### 端到端测试

端到端测试主要分为部署测试环境、调用函数进行测试以及移除测试环境这三步。

部署时使用 `git-qfzhang/alibaba-fc-deploy-action` 模版，部署完成后基于该模版输出的部署标准输出日志提取出函数的 `endpoint` 并以环境变量的形式传入到测试中，注意，这里获取到的日志文本与控制台输出的日志有所差异，其中不存在换行符。

```yaml
- id: deploy-test
  name: Deploying test
  uses: git-qfzhang/alibaba-fc-deploy-action@main
  env:
    PROVIDER: ${{ env.PROVIDER }}
    REGION: ${{ env.REGION }}
    SERVICE_NAME: ${{ env.TEST_SERVICE_NAME }}
    NODEJS_FUNCTION_NAME: ${{ env.TEST_NODEJS_FUNCTION_NAME }}
  with: 
    working_directory: ${{ env.FC_CODE_URI }}
    projects: ${{ env.NODEJS_PROJECT_NAME }}
      
- name: Extracting endpoint to GITHUB_ENV
  run: | 
    statement=$( echo ${{ steps.deploy-test.outputs.deploy-logs }} | grep 'EndPoint:' | sed -e 's/.*EndPoint: //g' | sed -e 's/ Trigger:.*//g' | sed -e 's/^/ENDPOINT=/g' )
    echo "$statement" >> $GITHUB_ENV
```

上述步骤会在线上部署一套测试环境，测试环境对应的函数计算服务名称会在原服务名称后面加上 `-Test` 后缀，该服务只会创建一次，后续本案例测试环境的函数均部署在该服务下。

为了保证每次测试部署的测试环境中的函数唯一，将测试环境中的函数名称加上 `-${{ github.run_id }}` 后缀，`${{ github.run_id }}` 表示仓库中每个 GitHub Actions 运行的唯一编号，更多信息请参考[这里](https://docs.github.com/cn/free-pro-team@latest/actions/reference/context-and-expression-syntax-for-github-actions)。

端到端测试使用 `npm run e2e:test` 指令运行。

测试完成后会直接使用 `s remove trigger` 以及 `s remove function` 指令来清理触发器以及测试函数。

```yaml
- name: Removing test
  env:
    PROVIDER: ${{ env.PROVIDER }}
    REGION: ${{ env.REGION }}
    SERVICE_NAME: ${{ env.TEST_SERVICE_NAME }}
    NODEJS_FUNCTION_NAME: ${{ env.TEST_NODEJS_FUNCTION_NAME }}
  run: |
    sudo --preserve-env s ${{ env.NODEJS_PROJECT_NAME }} remove trigger
    sudo --preserve-env s ${{ env.NODEJS_PROJECT_NAME }} remove function
  working-directory: ${{ env.FC_CODE_URI }}
```

#### 部署发布

测试通过后，就可以借助 `git-qfzhang/alibaba-fc-deploy-action` 部署模版以及 `s publish` 指令进行部署发布。注意，部署发布时候需要将服务名以及函数名更改成生产环境中的名称。

## Java 案例

本案例是函数计算 Java 事件函数，函数内容可参考[这里](https://help.aliyun.com/document_detail/113518.html?spm=a2c4g.11174283.6.580.206852122CzKTw)。

本案例的 CI/CD 流程与 Nodejs 案例的流程的唯一不同之处在于需要进行两次构建操作，其余流程均相同，此处不再进行详细介绍，具体内容可参考文件[cicd.yml](.github/workflows/cicd.yml)中的`java-cicd-job`内容。

## 总结

本文以阿里巴巴函数计算为例介绍了如何使用 Serverless Devs 工具和 Github Actions 进行 Serverless 项目的 CI/CD，借助 Serverless Devs 的通用性，其他云平台的 Serverless 项目也可以基于本文提供的模版修改相应指令后实现 Serverless CI/CD。
