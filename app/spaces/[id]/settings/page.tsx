import { authOptions } from "@/lib/auth";
import { javaApiRequest } from "@/lib/java-api";
import SpaceSettingsView from "@/components/SpaceSettingsView";
import { getServerSession } from "next-auth";
import { notFound, redirect } from "next/navigation";

interface Params {
  params: { id: string };
}

export default async function SpaceSettingsPage({ params }: Params) {
  const session = await getServerSession(authOptions);
  if (!session?.user?.email) {
    redirect("/");
  }

  const spaceResponse = await javaApiRequest(`/api/spaces/${params.id}`);
  if (!spaceResponse.ok) {
    if (spaceResponse.status === 404) {
      notFound();
    } else if (spaceResponse.status === 403) {
      redirect("/");
    }
    throw new Error("Failed to load space");
  }
  const space = await spaceResponse.json();

  const activityResponse = await javaApiRequest(
    `/api/spaces/${params.id}/activity`
  );
  const activities = activityResponse.ok ? await activityResponse.json() : [];

  return (
    <div className="min-h-screen bg-gray-100 py-10 px-4 sm:px-8">
      <SpaceSettingsView
        space={space}
        activities={activities}
        currentUserEmail={session.user.email!}
      />
    </div>
  );
}

