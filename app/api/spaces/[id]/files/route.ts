import { NextRequest, NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import { getJavaApiHeaders } from "@/lib/java-api";

const JAVA_API_URL = process.env.JAVA_API_URL || "http://localhost:8080";

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const headers = await getJavaApiHeaders();
    const response = await fetch(`${JAVA_API_URL}/api/spaces/${params.id}/files`, {
      headers,
    });
    const data = await response.json();
    return NextResponse.json(data);
  } catch (error: any) {
    return NextResponse.json(
      { error: error.message || "Failed to fetch files" },
      { status: 500 }
    );
  }
}

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const formData = await request.formData();
    const headers = await getJavaApiHeaders();
    delete (headers as any)["Content-Type"]; // Let fetch set it for FormData

    const response = await fetch(`${JAVA_API_URL}/api/spaces/${params.id}/files`, {
      method: "POST",
      headers,
      body: formData,
    });
    const data = await response.json();
    return NextResponse.json(data, { status: 201 });
  } catch (error: any) {
    return NextResponse.json(
      { error: error.message || "Failed to upload file" },
      { status: 500 }
    );
  }
}

