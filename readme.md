+ 마이크로 서비스 상태 체크
```
❯ curl localhost:8080/actuator/health -s | jq . 
```

---
## 이벤트 기반 비동기 서비스
### 스프링 클라우드 스트림 구성
+ 소비자 그룹
  - message consumer 의 인스턴스 수를 늘리면, 모든 인스턴스가 같은 메시지를 소비한다는 문제가 존재
    - 이런 문제를 해결하려면 소비자 유형별로 하나의 인스턴스가 메시지를 처리하도록 해야 한다.  
    따라서 consumer group 을 도입하여 이를 해결할 수 있다.
      
+ 재시도 및 데드 레터(dead-letter) 대기열
  - consumer 가 메시지 처리에 실패하면 메시지는 실패한 소비자가 처리할 때까지 대기열로 다시 보내지거나 사라진다.
  - 결함 분석 및 수정을 위해 메시지를 다른 저장소로 이동하기 전에 `수행할 재시도 횟수`를 지정할 수 있다.
    - 실패한 메시지는 `데드 레터 대기열`이라는 전용 대기열로 이동된다.
  
+ 순서 보장 및 파티션
  - 파티션을 사용하면 `성능과 확장성`을 잃지 않으면서도 전송됐을 떄의 순서 그대로 메시지를 전송할 수 있다.
  - 메시지가 전송된 순서대로 메시지를 소비하고 처리해야 하는 경우엔 여러 개의 소비자 인스턴스를 사용해 처리 성능을 높일 수 없다.  
  즉, `소비자 그룹`을 사용할 수 없다. -> 메시지를 처리할 떄 발생하는 지연 시간이 지나치게 길어질 수 있다.
    
  - 대부분 엄격하게 순서를 지켜서 메시지를 처리해야 하는 경우는, 같은 비즈니스 엔티티(예: 같은 pk를 가진 entity)에 영향을 줄 때
    - 이를 해결하려면 하위 토픽이라고도 알려진 `파티션`을 도입해, 메시징 시스템이 같은 키를 기준으로 특정 파티션에 메시지를 배치한다.
    - 메시지 순서 보장을 위해, 소비자 그룹 안의 각 파티션마다 하나의 소비자 인스턴스가 배정된다
    
### 토픽당 2개의 파티션으로 rabbitMq  사용
```
❯ export COMPOSE_FILE=docker-compose-partitions.yml
❯ docker-compose build && docker-compose up 
```

### 토픽당 2개의 파티션으로 카프카로 대체해보기
```
❯ export COMPOSE_FILE=docker-compose-kafka.yml
❯ docker-compose build && docker-compose up 
```

+ 커맨드로 실행해 토픽 목록 확인
```
❯ docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --zookeeper zookeeper --list
```

+ `product` 토픽과 같은 특정 토픽의 파티션을 보려면
```
❯ docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --describe --zookeeper zookeeper --topic products
```

+ `product` 토픽과 같은 특정 토픽의 `모든 메시지`를 보려면
```
❯ docker-compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic products --from-beginning --timeout-ms 1000 
```

+  `product` 토픽의 파티션 1과 같은 특정 파티션의 모든 메시지를 보려면
````
❯ docker-compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic products --from-beginning --timeout-ms 1000 --partition 1
````
---
## 스프링 클라우드를 활용한 마이크로 서비스 관리

---
## 유레카와 리본을 사용한 서비스 검색
> DNS 기반 서비스 검색의 문제

마이크로 서비스 인스턴스의 DNS 이름이 같기만 하면, DNS 서버는   사용 가능한 인스턴스의 IP 주소 목록을 분석한다.
따라서 클라이언트는 라운드 로빈 방식으로 서비스 인스턴스를 호출할 수 있다.  

특정 마이크로 서비스의 인스턴스가 2 개라고 가정했을 떄, 해당 마이크로 서비스를 호출 시, 하나의 인스턴스 에서만 응답을 한다.  
그 이유는 DNS 클라이언트는 보통 `리졸브된 IP 주소를 캐시`하며, DNS 이름에 대응되는 IP 주소가 여러 개일지라도, 동작하는 첫번 째 IP 주소를 계속 사용한다.  

따라서 DNS 서버와 DNS 프로토콜은 동적으로 변하는 마이크로 서비스 인스턴스를 처리하는 데, 부적합하다.

### 검색 서비스 사용
+ 확장
  - 검색 서비스를 테스트하기 위한, review ms instance 2개 더 확장  
``
❯ docker-compose up -d --scale review = 3
``
    

+ 실행 후 `8761` 포트로 브라우저 실행 후, review 인스턴스 3개 확인  
  + 새 인스턴스가 언제 실행됐는지 확인하려면 `docker-compose log -f review` 커맨트 실행


+ 유레카 서비스가 제공하는 restapi
```
❯ curl -H "accept:application/json" localhost:8761/eureka/apps -s | jq -r .applications.application[].instance[].instanceId
```


+ 클라이언트 측 로드밸런서로 요청을 보낸 후 결과에 있는 review 서비스의 주소를 확인

````
❯ curl localhost:8080/product-composite/2 -s | jq -r .serviceAddresses.rev
````

---
## 스프링 클라우드 게이트웨이를 에지서버로 사용
### 에지 서버
일부 마이크로서비스만 시스템 환경 외부에 공개하고, 그외 마이크로서비스는 차단하는 게 바람직하다.  
이를 클라이언트와 마이크로서비스 사이에 `새 컴포넌트(에지서버)`를 추가하여 충족시킬 수 있다.

---
## API 접근 보안
### Oauth 2.0
`권한 부여`를 위해 광범위하게 사용되는 공개 표준으로, 사용자에게 권한을 위임받은 `서드파티 클라이언트 애플리케이션`이
사용자를 대신해 보안 리소스에 접근을 할 수 있게 한다.

+ Resource owner(자원 소유자) : 최종 사용자
+ Client : 최종 사용자의 권한을 위임받아, 보안 API를 호출하는 서드파티 애플리케이션
+ Resource Server(자원 서버) : 보호 대상 자원에 대한 API를 제공하는 서버.
+ Authorization server(권한 부여 서버) : 자원 소유자(최종 소유자)를 인증하고, 자원 소유자의 승인을 받아서 클라이언트에게 토큰을 발급한다.  
사용자 정보 관리 및 사용자의 인증은 Identity Provider(ID 제공자)에게 위임된다.
  
> 권한 부여 서버에 등록된 클라이언트는 Client Id와 Client Secret을 발급받는다.  
> 클라이언트는 암호와 마찬가지로 Client Secret을 보호해야 한다.  
> 클라이언트는 리다이렉트 URI를 등록해야 하며, 권한 부여 서버는 사용자 인증을 거쳐 발급한 Grant code(인증코드)와 Token(토큰)을 리다이렉트 URI로 전달한다.

### Oauth 승인 방식
해당 링크 참조 : https://cheese10yun.github.io/spring-oauth2-provider/

---
## 구성 중앙화

#### 구성 보안  
  - 구성 정보는 중요한 정보로 취급해야 하므로, 구성 정보를 전송하거나 저장할 떄는 보안에 신경 써야한다.  
  따라서 구성 서버가 에지 서버를 통해 외부로 노출될 이유가 없기 때문에, 상용 환경에서는 구성 서버에 대한 외부 접근을 차단해야 한다. 
  
#### 구성 전송 보안
 - 마이크로 서비스나 구성 서버 API 사용자의 구성 정보 요청은 HTTPS를 사용하는 에지 서버에 의해 도청으로 보호된다.

#### 구성 저장 보안
  - 구성 저장소 접근 권한이 있는 사람이 암호 등의 민감한 정보를 훔쳐가는 상황을 피하고자 구성 서버는 구성 정보를 암호화해서 디스크 저장한다.  
  구성 서버는 대칭 키와 비대칭 키를 모두 지원하는데, 비대칭 키가 더 안전하지만 관리는 어렵다.
  
---  
## Resilience4j 를 사용한 탄력성 개선
재시도와 서킷 브레이커 메커니즘은 마이크로 서비스와 같이 동기 방식으로 연결되는 소프트웨어 컴포넌트에 특히 유용하며,  
에지 서버를 제외한 모든 마이크로서비스 Resilience4j를 사용할 수 있다. (스프링 클라우드 게이트 웨이가 지원 X)  

## 서킷 브레이커 소개
+ 서킷 브레이커는 다량의 오류를 감지하면 서킷을 열어 새 호출을 받지 않는다.
+ 서킷 브레이커는 서킷이 열려 있을 때 빠른 실패 로직을 수행한다. `즉, 이어지는 호출에서 시간 초과 등으로 새로운 오류가 발생하지 않게 하며`, `폴백 메서드(fallback method)`로 호출을 리디렉션한다.  
  + 폴백 메서드에 다양한 비즈니스 로직을 적용하면, 로컬 캐시의 데이터를 반환하거나 즉각적인 오류 메시지를 반환하는 등의 최적화된 응답을 생성할 수 있다.  
  -> 의존하는 서비스의 응답 중단 때문에 마이크로서비스가 응답하지 못하게 되는 문제를 방지   
    :) 고부하 상황에서 특히 유용  
+ 시간이 지나면 서킷 브레이커는 반열림 상태로 전환돼 새로운 호출을 허용
  + 이를 통해 문제를 일으킨 원인이 사라졌는지 확인한다.
  + 서킷 브레이커는 새로운 오류를 감지하면 서킷을 다시 열고 빠른 실패 로직을 다시 수행
+ 마이크로서비스에 장애에 대한 `탄력성`을 부여하며, `동기 방식으로 통신하는 마이크로 서비스 시스템 환경의 필수 기능`

## 요약
서킷 브레이커는 서킷이 열려 있을 때 빠른 실패, 폴백 메서드를 작동시킨다. 이를 사용하여 마이크로서비스가 의존하는 `동기 서비스가 정상적으로 응답하지 않을 떄도 서비스가 응답 불능 상태에 빠지지 않게 할 수 있다.`  
또한 서킷이 반열림 상태에 있을 떄는 장애가 발생한 서비스가 정상 상태로 돌아왔는지 확인하고,  
다시 요청을 처리할 수 있도록 서킷을 닫아서 마이크로서비스의 탄력성을 높인다.

---
## 분산 추적
분산추적은 다음과 같은 개념을 사용한다.
+ 전체 workflow 에 대한 추적 정보 : trace 혹은 trace tree
  + 기본 작업 단위(트리의 일부분) : span
    + 하위 스팬으로 구성돼 trace tree 를 형성한다.
  
### 테스트
+ 접근 토큰 가져오기
```
❯ unset ACCESS_TOKEN  
❯ ACCESS_TOKEN=$(curl -k https://writer:secret@localhost:8443/oauth/token -d grant_type=password -d username=gongdel -d password=password -s | jq -r .access_token)
❯ echo $ACCESS_TOKEN 
```
#### 정상적인 API 요청 전송 (200반환)
````
❯ curl -H "Authorization: Bearer $ACCESS_TOKEN" -k https://localhost:8443/product-composite/2 -w "%{http_code}\n" -o /dev/null -s 
````

---
## 쿠버네티스 적용
### 기본 개념
쿠버네티스는 컨테이너를 실행하는 서버 클러스터(물리 혹은 가상)를 하나의 커다란 논리 서버로 만드는 컨테이너 오케스트레이터  
쿠버네티스 클러스터의 주요 목적 중 하나는 컨테이너를 배포 및 실행하면서 블루/그린 배포나 카나리아 배포 등의 기술을 사용해 무중단 롤링 업그레이드를 지원하는 것이다.
[배포전략](https://reference-m1.tistory.com/211)

### 쿠버네티스 객체
+ Node : 클러스터에 속한 가상 혹은 물리 서버
+ Pod : 쿠버네티스에 배포할 수 있는 컴포넌트의 최소 단위
  + 하나 혹은 여러 개의 컨테이너를 포함
+ Deployment : Pod를 배포하고 업그레이드
  + Pod의 생성과 모니터링을 ReplicaSet이 담당하도록 위임
+ ReplicaSet : 지정한 수의 Pod가 항상 실행되게 한다.
+ Service : 하나 혹은 여러 개의 Pod에 연결하고 사용하는 네트워크 엔드포인트
  + Service로 전송된 요청은 라운드 로빈 방식의 로드 밸런싱을 사용해, 사용 가능한 포드 중 하나로 전달
  + 기본적으로 Service는 `클러스터 Ip 주소`를 사용해 `클러스터 내부에만` 노출
  + 외부로 서비스를 노출하기 위해서는 `각 노드의 전용 포트` 혹은 `쿠버네티스와 연결된 외부 로드 밸런서`를 사용
+ Ingress : 쿠버네티스 클러스터의 Service에 대한 HTTP 기반의 외부 접근을 관리한다.
  + 예를들면 URL 경로나 호스트 이름 등의 HTTP 헤더를 기반으로 트래픽을 클러스터의 Service로 라우팅
  + Ingress 객체로 정의한 통신을 실제로 처리하려면, 클러스터에서 Ingress controller를 실행하고 있어야 한다.
+ Namespace : 쿠버네티스 클러스터의 리소스를 모으고 특정 수준으로 격리하고자 사용
  + Namespace에 속한 리소스는 이름이 고유해야 하지만, Namespace가 다른 경우에는 이름이 같을 수 있다.
+ ConfigMap : 컨테이너에서 사용하는 구성을 저장할 떄 사용
+ Secret : 자격 증명 등의 컨테이너에서 사용하는 민감한 정보를 저장할 떄 사용
  + API 서버 접근 권한이 있는 사람은 누구나 시크릿의 내용을 볼 수 있기 때문에, 이름이 의미하는 것처럼 안전하진 않음
+ DaemonSet : 클러스터 노드 집합의 각 노드마다 하나의 포드가 실행되게 한다.
