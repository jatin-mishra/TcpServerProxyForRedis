# TcpServerProxyForRedis
Built a high-performance TCP server in Java following a multi-stage concurrency model similar to the Spring Boot/Tomcat architecture:

* **Acceptor thread** handles incoming connections
* **Poller (selector) threads** manage non-blocking I/O using event-driven multiplexing
* **Worker thread pool** processes requests asynchronously

Acts as a **Redis proxy layer**, routing and executing commands with minimal latency, while abstracting direct client access.

Implements a **distributed rate limiter** at the TCP layer, enforcing request throttling before hitting downstream systems.

Designed for:

* Efficient connection handling under high concurrency (epoll/NIO-based)
* Backpressure and thread isolation across stages
* Reduced load on Redis via centralized control plane logic

Written in Java with focus on low-latency I/O, concurrency control, and system resilience.

