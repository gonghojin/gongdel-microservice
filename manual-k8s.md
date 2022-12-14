# 쿠버네티스 관련 메뉴얼
---

## 쿠버네티스에 마이크로서비스 배포
### 넷플릭스 유레카 -> 쿠버네티스 Service
쿠버네티스 Service 객체와 kube-proxy 기반의 검색기능을 내장하고 있다.
따라서 앞서 사용한 넷플릭스 유레카와 같은 별도의 검색 서비스는 배포할 필요가 없다.

---
### Kustomize
쿠버네트스 정의파일(yml)을 개발, 테스트, 준비, 상용 환경 등의 다양한 환경에 맞춰 사용자 정의할 때 사용하는 도구  

+ 도커 이미지 빌드
    + 일반적으로는 도커 레지스트르에 이미지를 올리고, 레지스트리에서 이미지를 가져오도록 쿠버네티스를 구성해야 한다.  
    하지만 로컬 단일 노드 클러스터를 사용하는 경우에는 아래처럼 미니큐브의 도커엔진을 가리키도록 설정한 후 , 도커 이미지를 빌드하는 게 간편한다.  
      이렇게 하면 쿠버네티스에서 도커 이미지를 바로 사용할 수 있다.
```
❯ eval $(minikube docker-env)
❯ ./gradlew build && docker-compose build 
```
---
#### 개발 환경 배포
+ 쿠버네티스에 배포
    + 마이크로서비스를 쿠버네티스에 배포하려면 먼저 네임스페이스, 컨피그 맵, 시크릿을 만들어야 한다.  
    배포를 수행한 후에는 디플로이먼트가 동작할 떄까지 기다렸다가 포드에 사용한 도커 이미지 및 포드가 제대로 배포됐는지 확인한다.

네임스페이스를 생성하고, kubectl의 기본 네임스페이스로 설정한다
````
❯ kubectl create namespace hands-on 
❯ kubectl config set-context $(kubectl config current-context) --namespace=hands-on  
````

구성 저장소와 암호화한 민감한 정보는 컨피그 맵에 저장하고, 구성 서버 접근을 위한 자격 증명과 암호화 키는 별도의 시크릿으로 나눠서 저장한다.
1. config-repo 폴더의 파일을 기반으로 구성 저장소를 위한 컨피그 맵을 생성한다.
````
❯ kubectl create configmap config-repo --from-file=config-repo/ --save-config
````

2. 구성 서버를 위한 시크릿을 생성한다.
````
❯ kubectl create secret generic config-server-secrets \                                                                                ─╯
> --from-literal=ENCRYPT_KEY=my-very-secure-encrypt-key \                      
> --from-literal=SPRING_SECURITY_USER_NAME=dev-usr \                           
> --from-literal=SRPING_SECURITY_USER_PASSWORD=dev-pwd \                       
> --save-config  
````

3. 구성 서버 클라이언트를 위한 시크릿을 생성한다.
````
❯ kubectl create secret generic config-client-credentials \                                                                            ─╯
> --from-literal=CONFIG_SERVER_USR=dev-usr \                                   
> --from-literal=CONFIG_SERVER_PWD=dev-pwd --save-config   
````

4. 라이브니스 프로브 제한 설정값에 위배되지 않도록, 도커 이미지 다운로드를 미리 다운로드한다.
````
❯ docker pull mysql:5.7
❯ docker pull mongo:3.6.9 
❯ docker pull rabbitmq:3.7.8-management 
❯ docker pull openzipkin/zipkin:2.12.9 
````

5. -k 스위치로 Kustomize를 활성하고 dev overlay를 사용해 개발 환경에 마이크로서비스를 배포한다.
````
❯ kubectl apply -k kubernetes/services/overlays/dev 
````

--- 
### 롤링 업그레이드 수행(p566)
업데이트를 수행하면 대개 업데이트 대상 컴포넌트가 잠시 중단된다. 하지만 운영 환경에서는 비가동 시간없이 업데이트를 배포할 수 있어야 한다.  
디플로이먼트 객체는 기본적으로 모든 업데이트를 롤링 업그레이드로 수행하도록 구성돼 있다.  

---
## 쿠버네티스로 기존 인프라 대체
+ 서비스 검색 디자인 패턴을 구현한 넷플릭스 유레카 -> 쿠버네티스의 Service
+ 스프링 클라우드 Config Server -> 쿠버네티스의 ConfigMap & Secret
+ 스프링 클라우드 게이트웨이 -> Ingress
+ HTTPS로 외부 통신 보호
  + 외부 API를 보호하고자 인증서를 사용하였는데, 인증서를 수동으로 관리하면 시간이 많이 걸리고 오류가 발생하기 쉽다.  
  -> `Cert Manager`로 해결 가능
    + Cert Manager는 Ingress에 의해 외부로 노출되는 HTTPS 엔드포인트의 인증서가 만료됐을 떄 이를 대체할 새 인증서를 프로비저닝
  
### 스프링 클라우드 컨피그 서버 대체
스프링 클라우드 컨피그 서버를 사용하면 모든 구성 정보를 한곳에 보관하거나, 깃을 사용해 버전관리를 하는 등 많은 이점이 있지만, 다른 애플리케이션과 마찬가지로 많은 양의 메모리를 소비하며 시작 비용 또한 상당하다.  
쿠버네티스 컨피그맵과 시크릿을 사용하면 이런 지연이 없기 떄문에 더 빠르게 자동화된 환경을 사용할 수 있다.  

### ㅅ프링 클라우드 게이트웨이 대체
스프링 클라우드 게이드웨이를 쿠버네티스에서 제공하는 Ingress 리소스로 대체해 배포가 필요한 자원 서비스의 수를 줄일 수 있다.  

스프링 클라우드 게이드웨이가 인그레스에 비해 풍부한 라우팅 기능을 제공하긴 하지만, 인그레스는 쿠버네티스 플랫폼에서 기본 제공하는 기능이며,  
Cert Manager를 사용해 인증서를 자동으로 프로비저닝하도록 확장할 수 있다는 장점이 있다.

또한 게이드웨이에 추가한 복합 상태 점검은 각 마이크로서비스의 디폴로이먼트 리소스에 라이브니스(liveness) 프로브와 레드니스(readiness) 프로브를 정의해 대체할 수 있다.  

