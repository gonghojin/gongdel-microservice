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

```