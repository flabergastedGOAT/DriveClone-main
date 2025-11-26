"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChangeEvent, useMemo, useState } from "react";

interface SpaceMember {
  email: string;
  role: "ADMIN" | "MEMBER";
  owner?: boolean;
  addedAt?: string;
}

interface Space {
  id: string;
  name: string;
  description?: string;
  adminEmail: string;
  members?: SpaceMember[];
}

interface Activity {
  id: string;
  userEmail: string;
  action: string;
  details?: string;
  timestamp: string;
}

interface Props {
  space: Space;
  activities: Activity[];
  currentUserEmail: string;
}

export default function SpaceSettingsView({
  space,
  activities,
  currentUserEmail,
}: Props) {
  const router = useRouter();
  const [name, setName] = useState(space.name ?? "");
  const [description, setDescription] = useState(space.description ?? "");
  const [members, setMembers] = useState<SpaceMember[]>(space.members ?? []);
  const [memberEmail, setMemberEmail] = useState("");
  const [membersOpen, setMembersOpen] = useState(true);
  const [activityOpen, setActivityOpen] = useState(false);
  const [localActivities, setLocalActivities] = useState<Activity[]>(activities);

  const isAdmin = useMemo(() => {
    if (space.adminEmail === currentUserEmail) return true;
    return (
      space.members?.some(
        (member) =>
          member.email === currentUserEmail && member.role === "ADMIN"
      ) ?? false
    );
  }, [space, currentUserEmail]);

  const refreshSpace = async () => {
    const res = await fetch(`/api/spaces/${space.id}`);
    if (res.ok) {
      const updated = await res.json();
      setName(updated.name ?? "");
      setDescription(updated.description ?? "");
      setMembers(updated.members ?? []);
    }
    const activityRes = await fetch(`/api/spaces/${space.id}/activity`);
    if (activityRes.ok) {
      const updatedActivity = await activityRes.json();
      setLocalActivities(Array.isArray(updatedActivity) ? updatedActivity : []);
    }
    router.refresh();
  };

  const handleSave = async () => {
    if (!isAdmin) return;
    const payload: Record<string, string> = {};
    if (name.trim() !== space.name) payload.name = name.trim();
    if ((description ?? "") !== (space.description ?? ""))
      payload.description = description;
    if (!Object.keys(payload).length) return;
    const res = await fetch(`/api/spaces/${space.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      alert(error.error || "Unable to update space");
      return;
    }
    await refreshSpace();
  };

  const handleDelete = async () => {
    if (!isAdmin) return;
    if (!confirm(`Delete "${space.name}"? This cannot be undone.`)) return;
    const res = await fetch(`/api/spaces/${space.id}`, {
      method: "DELETE",
    });
    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      alert(error.error || "Unable to delete space");
      return;
    }
    router.push("/");
    router.refresh();
  };

  const handleAddMember = async () => {
    if (!isAdmin || !memberEmail.trim()) return;
    const res = await fetch(`/api/spaces/${space.id}/members`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: memberEmail.trim() }),
    });
    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      alert(error.error || "Unable to add member");
      return;
    }
    setMemberEmail("");
    await refreshSpace();
  };

  const handleRemoveMember = async (email: string) => {
    if (!isAdmin) return;
    if (space.adminEmail === email) {
      alert("You cannot remove the workspace owner.");
      return;
    }
    if (!confirm(`Remove ${email} from this workspace?`)) return;
    const res = await fetch(
      `/api/spaces/${space.id}/members/${encodeURIComponent(email)}`,
      { method: "DELETE" }
    );
    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      alert(error.error || "Unable to remove member");
      return;
    }
    await refreshSpace();
  };

  const handleChangeRole = async (email: string, role: "ADMIN" | "MEMBER") => {
    if (!isAdmin) return;
    const res = await fetch(
      `/api/spaces/${space.id}/members/${encodeURIComponent(email)}`,
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ role }),
      }
    );
    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      alert(error.error || "Unable to update role");
      return;
    }
    await refreshSpace();
  };

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">
            <Link href="/" className="text-blue-600 hover:text-blue-700">
              ← Back to dashboard
            </Link>
          </p>
          <h1 className="text-3xl font-bold text-gray-900 mt-2">
            {space.name}
          </h1>
          <p className="text-sm text-gray-500">
            Manage workspace settings and members
          </p>
        </div>
        <button
          onClick={handleDelete}
          disabled={!isAdmin}
          className="bg-red-600 disabled:bg-red-200 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition"
        >
          Delete Workspace
        </button>
      </div>

      <section className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          Space Settings
        </h2>
        <div className="grid gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Name
            </label>
            <input
              type="text"
              value={name}
              disabled={!isAdmin}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              value={description}
              disabled={!isAdmin}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 h-28"
            />
          </div>
        </div>
        <div className="mt-4 flex gap-3">
          <button
            onClick={handleSave}
            disabled={!isAdmin}
            className="bg-blue-600 disabled:bg-blue-200 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
          >
            Save Changes
          </button>
          <button
            onClick={refreshSpace}
            className="border border-gray-300 px-4 py-2 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
          >
            Refresh
          </button>
        </div>
      </section>

      <section className="bg-white border border-gray-200 rounded-xl shadow-sm">
        <button
          onClick={() => setMembersOpen((prev) => !prev)}
          className="w-full flex items-center justify-between px-6 py-4 text-left"
        >
          <div>
            <p className="text-lg font-semibold text-gray-900">Members</p>
            <p className="text-sm text-gray-500">
              Invite teammates, manage roles, or remove access.
            </p>
          </div>
          <span className="text-sm text-gray-500">
            {membersOpen ? "Hide" : "Show"}
          </span>
        </button>
        {membersOpen && (
          <div className="px-6 pb-6 space-y-4">
            {isAdmin && (
              <div className="flex flex-col md:flex-row gap-3">
                <input
                  type="email"
                  value={memberEmail}
                  onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    setMemberEmail(e.target.value)
                  }
                  placeholder="user@example.com"
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  onClick={handleAddMember}
                  className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                >
                  Invite
                </button>
              </div>
            )}
            <div className="space-y-3">
              {members.length === 0 ? (
                <p className="text-sm text-gray-500">No members yet.</p>
              ) : (
                members.map((member) => (
                  <div
                    key={member.email}
                    className="p-3 border border-gray-200 rounded-lg flex flex-col md:flex-row md:items-center md:justify-between gap-3"
                  >
                    <div>
                      <p className="font-medium text-gray-900">
                        {member.email}
                        {member.owner && (
                          <span className="ml-2 text-xs text-yellow-800 bg-yellow-100 px-2 py-0.5 rounded-full">
                            Owner
                          </span>
                        )}
                      </p>
                      <p className="text-xs text-gray-500">
                        Role: {member.role}
                      </p>
                    </div>
                    {isAdmin && !member.owner && (
                      <div className="flex items-center gap-3">
                        <select
                          value={member.role}
                          onChange={(e) =>
                            handleChangeRole(
                              member.email,
                              e.target.value as "ADMIN" | "MEMBER"
                            )
                          }
                          className="border border-gray-300 rounded-lg px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                          <option value="MEMBER">Member</option>
                          <option value="ADMIN">Admin</option>
                        </select>
                        <button
                          onClick={() => handleRemoveMember(member.email)}
                          className="text-sm text-red-600 hover:text-red-700"
                        >
                          Remove
                        </button>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </section>

      <section className="bg-white border border-gray-200 rounded-xl shadow-sm">
        <button
          onClick={() => setActivityOpen((prev) => !prev)}
          className="w-full flex items-center justify-between px-6 py-4 text-left"
        >
          <div>
            <p className="text-lg font-semibold text-gray-900">
              Activity Log
            </p>
            <p className="text-sm text-gray-500">
              Track the latest actions within this workspace.
            </p>
          </div>
          <span className="text-sm text-gray-500">
            {activityOpen ? "Hide" : "Show"}
          </span>
        </button>
        {activityOpen && (
          <div className="px-6 pb-6 space-y-3">
            {localActivities.length === 0 ? (
              <p className="text-sm text-gray-500">No activity recorded yet.</p>
            ) : (
              localActivities.map((activity) => (
                <div
                  key={activity.id}
                  className="border-l-4 border-blue-500 pl-4 py-2 bg-gray-50 rounded-r-lg"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <span className="text-sm font-medium text-gray-900">
                        {activity.userEmail}
                      </span>
                      <span className="text-sm text-gray-600">
                        {activity.action}
                      </span>
                      {activity.details && (
                        <span className="text-sm text-gray-500">
                          {activity.details}
                        </span>
                      )}
                    </div>
                    <span className="text-xs text-gray-500">
                      {formatDateTime(activity.timestamp)}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </section>
    </div>
  );
}

function formatDateTime(timestamp: string) {
  const date = new Date(timestamp);
  return date.toLocaleString();
}

