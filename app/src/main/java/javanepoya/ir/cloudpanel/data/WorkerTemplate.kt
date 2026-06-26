package javanepoya.ir.cloudpanel.data

data class WorkerTemplate(
    val name: String,
    val nameFa: String,
    val description: String,
    val descriptionFa: String,
    val code: String
) {
    companion object {
        val templates = listOf(
            WorkerTemplate(
                name = "Hello World",
                nameFa = "سلام دنیا",
                description = "Simple Cloudflare Worker starter script returning plain text.",
                descriptionFa = "اسکریپت ساده آغازین کلودفلر ورکر که متن ساده برمی‌گرداند.",
                code = """
                    // Cloudflare Worker: Simple Hello World
                    export default {
                      async fetch(request, env, ctx) {
                        return new Response('Hello World from Cloudflare Worker!', {
                          headers: { 'content-type': 'text/plain' },
                        });
                      },
                    };
                """.trimIndent()
            ),
            WorkerTemplate(
                name = "API Proxy",
                nameFa = "پروکسی API",
                description = "Proxy requests to external target API, modifying paths or headers.",
                descriptionFa = "هدایت درخواست‌ها به یک ای‌پی‌آی خارجی با امکان تغییر مسیر یا هدرها.",
                code = """
                    // Cloudflare Worker: API Proxy / Reverse Proxy
                    export default {
                      async fetch(request, env, ctx) {
                        const url = new URL(request.url);
                        
                        // Define target backend host
                        const targetHost = "api.example.com";
                        
                        // Reconstruct URL
                        const targetUrl = "https://" + targetHost + url.pathname + url.search;
                        
                        // Copy headers and set host header correctly
                        const modifiedHeaders = new Headers(request.headers);
                        modifiedHeaders.set("Host", targetHost);
                        modifiedHeaders.set("X-Proxied-By", "Cloudflare-Worker");
                        
                        const modifiedRequest = new Request(targetUrl, {
                          method: request.method,
                          headers: modifiedHeaders,
                          body: request.method !== "GET" && request.method !== "HEAD" ? request.body : null,
                          redirect: "manual"
                        });
                        
                        try {
                          return await fetch(modifiedRequest);
                        } catch (err) {
                          return new Response("Proxy Error: " + err.message, { status: 502 });
                        }
                      }
                    };
                """.trimIndent()
            ),
            WorkerTemplate(
                name = "Redirect",
                nameFa = "تغییر مسیر (Redirect)",
                description = "Redirect requests from old URLs to new domains, or HTTP to HTTPS.",
                descriptionFa = "تغییر مسیر درخواست‌ها از آدرس‌های قدیمی به جدید، یا تبدیل HTTP به HTTPS.",
                code = """
                    // Cloudflare Worker: Request Redirector
                    export default {
                      async fetch(request, env, ctx) {
                        const url = new URL(request.url);
                        
                        // Force HTTPS
                        if (url.protocol === "http:") {
                          return Response.redirect("https://" + url.hostname + url.pathname + url.search, 301);
                        }
                        
                        // Example: Redirect specific paths
                        if (url.pathname === "/old-page" || url.pathname === "/about-us") {
                          return Response.redirect("https://" + url.hostname + "/about", 301);
                        }
                        
                        // Example: Domain redirection
                        // if (url.hostname === "old-domain.com") {
                        //   return Response.redirect("https://new-domain.com" + url.pathname + url.search, 301);
                        // }
                        
                        return fetch(request);
                      }
                    };
                """.trimIndent()
            ),
            WorkerTemplate(
                name = "Maintenance Page",
                nameFa = "صفحه در دست تعمیر",
                description = "Show an elegant custom HTML maintenance page with 503 service unavailable.",
                descriptionFa = "نمایش یک صفحه وب شیک برای حالت تعمیرات با وضعیت 503 غیرقابل دسترس.",
                code = """
                    // Cloudflare Worker: Elegant Maintenance Page
                    export default {
                      async fetch(request, env, ctx) {
                        const maintenanceHtml = `
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Site Under Maintenance</title>
                            <style>
                                body {
                                    background: radial-gradient(circle at center, #1e1b4b 0%, #0f172a 100%);
                                    color: #f8fafc;
                                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    height: 100vh;
                                    margin: 0;
                                    overflow: hidden;
                                }
                                .container {
                                    text-align: center;
                                    padding: 40px;
                                    border: 1px solid rgba(249, 115, 22, 0.2);
                                    border-radius: 16px;
                                    background: rgba(15, 23, 42, 0.8);
                                    backdrop-filter: blur(8px);
                                    box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                                    max-width: 500px;
                                }
                                h1 {
                                    font-size: 3rem;
                                    margin-bottom: 10px;
                                    background: linear-gradient(to right, #f97316, #fb923c);
                                    -webkit-background-clip: text;
                                    -webkit-text-fill-color: transparent;
                                }
                                p {
                                    color: #94a3b8;
                                    font-size: 1.1rem;
                                    line-height: 1.6;
                                }
                                .logo {
                                    font-size: 4rem;
                                    animation: spin 4s linear infinite;
                                    display: inline-block;
                                    margin-bottom: 20px;
                                }
                                @keyframes spin {
                                    100% { transform: rotate(360deg); }
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="logo">⚙️</div>
                                <h1>Under Maintenance</h1>
                                <p>We are currently upgrading our systems to serve you better. We'll be back online in a few minutes.</p>
                                <p style="font-size: 0.9rem; color: #fb923c;">Thank you for your patience!</p>
                            </div>
                        </body>
                        </html>
                        `;
                        
                        return new Response(maintenanceHtml, {
                          status: 503,
                          headers: {
                            "Content-Type": "text/html; charset=UTF-8",
                            "Retry-After": "300"
                          }
                        });
                      }
                    };
                """.trimIndent()
            )
        )
    }
}
