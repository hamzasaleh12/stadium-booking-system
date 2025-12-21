package com.hamza.stadiumbooking.welcome;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class WelcomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Stadium Booking API</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
                            height: 100vh;
                            margin: 0;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            color: #333;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 15px;
                            box-shadow: 0 10px 25px rgba(0,0,0,0.2);
                            text-align: center;
                            max-width: 520px;
                            width: 90%;
                        }
                        h1 { color: #1e3c72; margin-bottom: 10px; }
                        p { color: #666; margin-bottom: 20px; }
                        .btn-group { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
                        .btn {
                            text-decoration: none;
                            padding: 12px 25px;
                            border-radius: 8px;
                            font-weight: bold;
                            transition: all 0.3s ease;
                            display: inline-block;
                        }
                        .btn-swagger { background-color: #85ea2d; color: #1e3c72; }
                        .btn-swagger:hover { background-color: #74ce27; transform: translateY(-2px); }
                        .btn-health { background-color: #f4f4f4; color: #555; }
                        .btn-health:hover { background-color: #e2e2e2; }
                        .creds { background: #f7f9fc; border-radius: 10px; padding: 14px; font-size: 0.95em; color: #444; }
                        .creds h3 { margin: 0 0 8px 0; color: #1e3c72; }
                        .creds ul { list-style: none; padding: 0; margin: 0; }
                        .creds li { margin: 4px 0; }
                        footer { margin-top: 22px; font-size: 0.8em; color: #aaa; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Stadium Booking API 🚀</h1>
                        <p>Welcome, The backend service is running successfully on Railway.</p>

                        <div class="btn-group">
                            <a href="/swagger-ui/index.html" class="btn btn-swagger">Explore API Docs (Swagger)</a>
                            <a href="/actuator/health" class="btn btn-health">Check System Health</a>
                        </div>

                        <div class="creds">
                            <h3>Test Accounts (Swagger)</h3>
                            <ul>
                                <li>Admin: admin@gmail.com / Admin@1234</li>
                                <li>Manager: manager@gmail.com / Manager@1234</li>
                                <li>Player: player@gmail.com / Player@1234</li>
                            </ul>
                        </div>

                        <footer>Developed by Hamza Saleh &copy; 2025</footer>
                    </div>
                </body>
                </html>
                """;
    }
}