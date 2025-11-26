import { NextRequest, NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import { javaApiRequest } from "@/lib/java-api";

export async function DELETE(
  request: NextRequest,
  { params }: { params: { id: string; email: string } }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const encodedEmail = encodeURIComponent(params.email);
    const response = await javaApiRequest(
      `/api/spaces/${params.id}/members/${encodedEmail}`,
      {
        method: "DELETE",
      }
    );
    const data = await response.json();
    return NextResponse.json(data);
  } catch (error: any) {
    return NextResponse.json(
      { error: error.message || "Failed to remove member" },
      { status: 500 }
    );
  }
}

export async function PUT(
  request: NextRequest,
  { params }: { params: { id: string; email: string } }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();
    const encodedEmail = encodeURIComponent(params.email);
    const response = await javaApiRequest(
      `/api/spaces/${params.id}/members/${encodedEmail}`,
      {
        method: "PUT",
        body: JSON.stringify(body),
      }
    );
    const data = await response.json();
    return NextResponse.json(data);
  } catch (error: any) {
    return NextResponse.json(
      { error: error.message || "Failed to update member role" },
      { status: 500 }
    );
  }
}

