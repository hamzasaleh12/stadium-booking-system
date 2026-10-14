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
                    <title>Stadium Booking API | SBS</title>
                    <style>
                        :root {
                            --primary-gradient: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
                            --card-bg: rgba(255, 255, 255, 0.95);
                            --text-main: #1a1a1a;
                            --text-muted: #6b7280;
                            --accent: #10b981;
                            --accent-hover: #059669;
                            --secondary-btn: #f3f4f6;
                            --secondary-btn-hover: #e5e7eb;
                        }

                        body {
                            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                            background: var(--primary-gradient);
                            height: 100vh;
                            margin: 0;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            color: var(--text-main);
                            overflow: hidden;
                        }

                        .container {
                            background: var(--card-bg);
                            padding: 3rem;
                            border-radius: 20px;
                            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
                            text-align: center;
                            max-width: 550px;
                            width: 90%;
                            backdrop-filter: blur(10px);
                            border: 1px solid rgba(255,255,255,0.1);
                        }

                        .icon-wrapper {
                            background: #e0f2fe;
                            width: 80px;
                            height: 80px;
                            border-radius: 50%;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            margin: 0 auto 1.5rem;
                            font-size: 2.5rem;
                        }

                        h1 {
                            color: var(--text-main);
                            margin: 0 0 0.5rem 0;
                            font-size: 1.8rem;
                            font-weight: 700;
                        }

                        .status-badge {
                            display: inline-block;
                            background: #dcfce7;
                            color: #166534;
                            padding: 0.25rem 0.75rem;
                            border-radius: 999px;
                            font-size: 0.875rem;
                            font-weight: 600;
                            margin-bottom: 1.5rem;
                            border: 1px solid #bbf7d0;
                        }

                        .status-badge::before {
                            content: '';
                            display: inline-block;
                            width: 8px;
                            height: 8px;
                            background-color: #16a34a;
                            border-radius: 50%;
                            margin-right: 6px;
                            animation: pulse 2s infinite;
                        }

                        @keyframes pulse {
                            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(22, 163, 74, 0.7); }
                            70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(22, 163, 74, 0); }
                            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(22, 163, 74, 0); }
                        }

                        p {
                            color: var(--text-muted);
                            margin: 0 0 2rem 0;
                            line-height: 1.6;
                            font-size: 1rem;
                        }

                        .btn-group {
                            display: flex;
                            flex-direction: column;
                            gap: 1rem;
                            margin-bottom: 2rem;
                        }

                        .btn {
                            text-decoration: none;
                            padding: 0.875rem 1.5rem;
                            border-radius: 10px;
                            font-weight: 600;
                            transition: all 0.2s ease;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            gap: 8px;
                        }

                        .btn-primary {
                            background-color: var(--accent);
                            color: white;
                            box-shadow: 0 4px 6px rgba(16, 185, 129, 0.2);
                        }

                        .btn-primary:hover {
                            background-color: var(--accent-hover);
                            transform: translateY(-2px);
                            box-shadow: 0 6px 12px rgba(16, 185, 129, 0.3);
                        }

                        .btn-secondary {
                            background-color: var(--secondary-btn);
                            color: var(--text-main);
                        }

                        .btn-secondary:hover {
                            background-color: var(--secondary-btn-hover);
                            transform: translateY(-2px);
                        }

                        .info-box {
                            background: #f8fafc;
                            border: 1px dashed #cbd5e1;
                            border-radius: 12px;
                            padding: 1.25rem;
                            text-align: left;
                            font-size: 0.9rem;
                            color: #475569;
                        }

                        .info-box strong {
                            color: #0f172a;
                            display: block;
                            margin-bottom: 0.5rem;
                        }

                        .info-box a {
                            color: #2563eb;
                            text-decoration: none;
                            font-weight: 500;
                        }

                        .info-box a:hover {
                            text-decoration: underline;
                        }

                        footer {
                            margin-top: 2rem;
                            font-size: 0.85rem;
                            color: #9ca3af;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon-wrapper">🏟️</div>
                        <h1>Stadium Booking API</h1>
                        <div class="status-badge">System Online</div>

                        <p>The backend core service is running and ready to accept connections. This API handles secure authentication, concurrent bookings, and stadium management.</p>

                        <div class="btn-group">
                            <a href="/swagger-ui/index.html" class="btn btn-primary">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"></path></svg>
                                Explore API Docs (Swagger)
                            </a>
                            <a href="/actuator/health" class="btn btn-secondary">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0z"></path></svg>
                                Check System Health
                            </a>
                        </div>

                        <div class="info-box">
                            <strong>Architecture & Testing</strong>
                            To understand the system's architecture or get test credentials for the Swagger UI, please refer to the <a href="https://github.com/hamzasaleh12/stadium-booking-system" target="_blank" rel="noopener noreferrer">official README on GitHub</a>.
                        </div>

                        <footer>Developed by Hamza Saleh &copy; 2026</footer>
                    </div>
                </body>
                </html>
                """;
    }
}