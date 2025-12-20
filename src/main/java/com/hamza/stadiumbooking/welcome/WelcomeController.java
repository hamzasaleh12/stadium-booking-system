package com.hamza.stadiumbooking.welcome;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class WelcomeController {

    @GetMapping("/")
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
                            max-width: 500px;
                            width: 90%;
                        }
                        h1 { color: #1e3c72; margin-bottom: 10px; }
                        p { color: #666; margin-bottom: 30px; }
                        .btn-group { display: flex; flex-direction: column; gap: 15px; }
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
                        footer { margin-top: 30px; font-size: 0.8em; color: #aaa; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Stadium Booking API 🚀</h1>
                        <p>Welcome, Hamza! Your backend service is running successfully on Railway.</p>
               
                        <div class="btn-group">
                            <a href="/swagger-ui/index.html" class="btn btn-swagger">Explore API Docs (Swagger)</a>
                            <a href="/actuator/health" class="btn btn-health">Check System Health</a>
                        </div>

                        <footer>Developed by Hamza Saleh &copy; 2025</footer>
                    </div>
                </body>
                </html>
               \s""";
    }
}