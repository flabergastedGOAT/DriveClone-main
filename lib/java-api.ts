// Helper functions to communicate with Java backend API
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

const JAVA_API_URL = process.env.JAVA_API_URL || "http://localhost:8080";

export async function getJavaApiHeaders() {
  const session = await getServerSession(authOptions);
  if (!session?.user?.email) {
    throw new Error("Unauthorized");
  }

  // Create a JWT token for Java backend
  // The Java backend will verify this token
  const token = Buffer.from(
    JSON.stringify({
      email: session.user.email,
      name: session.user.name,
      id: session.user.id,
    })
  ).toString("base64");

  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
}

export async function javaApiRequest(
  endpoint: string,
  options: RequestInit = {}
) {
  const headers = await getJavaApiHeaders();
  const url = `${JAVA_API_URL}${endpoint}`;

  const response = await fetch(url, {
    ...options,
    headers: {
      ...headers,
      ...options.headers,
    },
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Java API error: ${error}`);
  }

  return response;
}

