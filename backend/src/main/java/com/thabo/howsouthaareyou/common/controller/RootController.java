package com.thabo.howsouthaareyou.common.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>DAFLAME</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f5f5f5;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            padding: 20px;
                            box-sizing: border-box;
                        }
                        .container {
                            background: white;
                            border-radius: 16px;
                            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                            padding: 40px;
                            max-width: 500px;
                            width: 100%;
                            text-align: center;
                        }
                        h1 {
                            color: #333;
                            font-size: 3rem;
                            margin: 0 0 24px 0;
                            letter-spacing: 2px;
                        }
                        .status {
                            display: inline-flex;
                            align-items: center;
                            gap: 8px;
                            background: #e8f5e9;
                            color: #2e7d32;
                            padding: 10px 20px;
                            border-radius: 24px;
                            font-weight: 600;
                            margin-bottom: 20px;
                        }
                        .dot {
                            width: 10px;
                            height: 10px;
                            background: #4caf50;
                            border-radius: 50%;
                            animation: pulse 2s infinite;
                        }
                        @keyframes pulse {
                            0%, 100% { opacity: 1; }
                            50% { opacity: 0.5; }
                        }
                        .quote {
                            color: #999;
                            font-size: 0.95rem;
                            font-style: italic;
                            margin: 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>DAFLAME</h1>
                        <div class="status">
                            <span class="dot"></span>
                            Southa Server is Operational
                        </div>
                        <p class="quote">"The One God Keeps Blessing"</p>
                    </div>
                </body>
                </html>
                """;
    }
}