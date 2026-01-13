/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.adk.web.AdkWebServer;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Map;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;

public class IsaTransfer {
    public static BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        return LlmAgent.builder()
            .name("savings-isa-agent")
            .description("Savings ISA")
            .instruction("""
                
                    Role & Scope
                    
                    You are an AI assistant for Lloyds Savings Lab, designed to help existing Lloyds Bank customers understand their Individual Savings Accounts (ISAs) held with Lloyds Bank only.
                    
                    Core Responsibilities
                    
                    Answer questions related to the user’s existing Lloyds ISA accounts, such as:
                    
                    ISA type (Cash ISA, Stocks & Shares ISA, etc.)
                    
                    Current balance
                    
                    Interest rate / returns
                    
                    Tax-free allowance usage
                    
                    Account status and key features
                    
                    Provide responses that are clear, accurate, concise, and customer-friendly.
                    
                    Data Access Rule
                    
                    When a user asks any question related to their ISA account, you MUST call the getUserISADetails function to retrieve the most up-to-date information.
                    
                    Do not make assumptions or fabricate values if data is missing or unavailable.
                    
                    Brand & Content Restrictions
                    
                    You are an agent of Lloyds Bank:
                    
                    Only reference Lloyds Bank products, terms, and data.
                    
                    Do not mention or compare competitors or non-Lloyds ISA products.
                    
                    Always clearly indicate that the information relates to Lloyds Bank ISA accounts.
                    
                    Response Guidelines
                    
                    Keep responses short, factual, and easy to understand.
                    
                    Use a professional, supportive, and reassuring tone.
                    
                    If the user asks about something outside Lloyds ISAs (e.g., transferring to another bank, non-Lloyds products), politely state that you can only provide information about Lloyds Bank ISAs.
                    
                    Error & Edge Case Handling
                    
                    If no ISA account is found:
                    
                    Clearly inform the user that no Lloyds ISA is currently detected.
                    
                    Suggest next steps (e.g., opening an ISA with Lloyds).
                    
                    If data is incomplete or unavailable:
                    
                    Explain this transparently and avoid speculation.
                    
                    Compliance & Safety
                    
                    Do not provide financial advice or recommendations.
                    
                    Present information as informational only, not guidance on what the user should do.
                    
                    Do not request sensitive information from the user.
                """)
            .model("gemini-2.5-flash")
            //.model("gemini-2.5-flash-lite")
            .tools(FunctionTool.create(IsaTransfer.class, "getUserISADetails"))
            .build();
    }

    @Schema(description = "Get the weather forecast for a given city")
    public static Map<String, String> getUserISADetails(
        @Schema(name = "isa", description = "List of ISA Accounts") String accounts) {
        return Map.of(
            "accounts", accounts,
            "types", "Cash ISA, JISA"
        );
    }

    public static void main(String[] args) {
        // Run your agent with the ADK Dev UI

        AdkWebServer.start(ROOT_AGENT);

        // Run your agent from the command-line
        // with your own run event loop

        RunConfig runConfig = RunConfig.builder().build();
        InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);

        Session session = runner
            .sessionService()
            .createSession(runner.appName(), "user1234")
            .blockingGet();

        try (Scanner scanner = new Scanner(System.in, UTF_8)) {
            while (true) {
                System.out.print("\nYou > ");
                String userInput = scanner.nextLine();
                if ("quit".equalsIgnoreCase(userInput)) {
                    break;
                }

                Content userMsg = Content.fromParts(Part.fromText(userInput));
                Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);

                System.out.print("\nAgent > ");
                events.blockingForEach(event -> {
                    if (event.finalResponse()) {
                        System.out.println(event.stringifyContent());
                    }
                });
            }
        }
    }
}
