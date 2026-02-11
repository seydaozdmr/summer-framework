# Summer Framework Production Checklist

Bu checklist, Summer Framework tabanli bir servisi production ortaminda daha guvenli ve olceklenebilir sekilde calistirmak icin hazirlandi.

## 1. Runtime Konfigurasyonu

- `server.port` degeri ortam bazli dogru ayarlandi.
- `summer.server.max-concurrent-requests` bilincli secildi.
- `summer.server.core-threads` ve `summer.server.max-threads` CPU kapasitesine uygun.
- `summer.server.queue-capacity` gecikme hedeflerine gore secildi.
- `summer.server.keep-alive-seconds` gereksinime gore ayarlandi.
- `summer.server.rejection-policy` stratejik secildi:
  - `ABORT` -> fail-fast
  - `CALLER_RUNS` -> throughput artabilir ama latency dalgalanir
  - `DISCARD_OLDEST` -> eski kuyruk item'lari atilir
- `summer.server.request-timeout-millis` timeout politikasina uygun.
- `summer.server.socket-backlog` sistem limitiyle uyumlu.

## 2. Capacity Planlama

- `maxConcurrentRequests`, `maxThreads` ile uyumlu planlandi.
- Bilincli fail-fast istendiginde `maxConcurrentRequests < maxThreads` secildi.
- Fail-fast istenmiyorsa `maxConcurrentRequests >= maxThreads` degerlendirildi.
- P95/P99 latency hedefine gore thread ve queue ayarlari yuk testi ile dogrulandi.

## 3. OS ve Kernel Ayarlari

- OS backlog limiti kontrol edildi (`somaxconn` vb).
- Process open file descriptor limiti (`ulimit -n`) yeterli.
- Firewall / security group kurallari dogru.
- TCP keepalive ve timeout ayarlari ortam standartlariyla uyumlu.
- Reverse proxy/load balancer timeout'lari uygulama timeout'lariyla celismiyor.

## 4. Timeout ve Iptal Davranisi

- `request-timeout-millis` aktifse handler kodlari interrupt'a duyarlilik acisindan gozden gecirildi.
- Timeout oldugunda arka planda yarim kalan islemler icin telafi/cleanup stratejisi var.
- Uzun sureli islemler icin ayri asenkron tasarim dusunuldu.

## 5. Queue ve Overload Davranisi

- Socket backlog doldugunda beklenen istemci davranisi biliniyor (connect timeout/refused).
- Executor queue doldugunda davranis (`503` veya policy etkisi) test edildi.
- OverloadGuard limitine ulasildiginda `503` davranisi yuk altinda dogrulandi.
- Queue buyuklugunun sonsuz buyume yaratmadigi dogrulandi.

## 6. Gozlemlenebilirlik (Observability)

- En az su metrikler toplaniyor:
  - active thread sayisi
  - queue doluluk oranlari
  - reject sayisi (`503`)
  - timeout sayisi (`504`)
  - request latency (p50/p95/p99)
  - hata oranlari (`4xx/5xx`)
- Structured log formati kullaniliyor.
- Her request icin correlation/request-id propagasyonu var.
- Alarm kurallari tanimli:
  - 503 spike
  - 504 spike
  - p99 latency artisi

## 7. Guvenlik ve Dogrulama

- Input boyut limiti ve dogrulama kurallari tanimli.
- Hassas bilgilerin log'lanmasi engellendi.
- CORS/payload/header politikasi ihtiyaca gore net.
- Acik endpoint'ler icin authentication/authorization katmani degerlendirildi.

## 8. Surdurulebilir Operasyon

- Graceful shutdown davranisi test edildi.
- Deployment sonrasi smoke test adimlari tanimli.
- Rollback plani var.
- Konfigurasyon degisiklikleri version control altinda.
- Incident runbook hazir:
  - 503 artisi
  - 504 artisi
  - saturasyon ve kuyruk taskinligi

## 9. Performans Testleri

- Baseline load test yapildi (normal trafik).
- Stress test yapildi (overload senaryosu).
- Soak test yapildi (uzun sureli calisma).
- Farkli `queueCapacity/rejectionPolicy/maxConcurrentRequests` kombinasyonlari karsilastirildi.

## 10. Release Kontrolu

- Yeni surumde backward compatibility notlari yazildi.
- README/guide dokumanlari guncel.
- Artifact version bump yapildi ve publish basarili.
- Tuketici projede yeni version smoke test edildi.

## 11. Hedeflenen Calisma Prensibi (Kisa Ozet)

Sira:

1. socket backlog (kernel)
2. ioExecutor queue/thread (user-space)
3. semaphore gate (`maxConcurrentRequests`)
4. optional invocation executor (timeout aktifse)
5. response

Bu zincirde her adimin limiti farklidir; tuning her katman icin ayri yapilmalidir.
