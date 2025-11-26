"use client";

import Link from "next/link";
import { useSession, signOut } from "next-auth/react";
import { ChangeEvent, useEffect, useState } from "react";

interface Space {
  id: string;
  name: string;
  description?: string;
  adminEmail: string;
}

interface SpaceFile {
  id: string;
  originalFilename: string;
  size: number;
  uploaderEmail: string;
  uploadedAt: string;
}

interface Activity {
  id: string;
  userEmail: string;
  action: string;
  details?: string;
  timestamp: string;
}

export default function Dashboard() {
  const { data: session } = useSession();
  const [spaces, setSpaces] = useState<Space[]>([]);
  const [selectedSpace, setSelectedSpace] = useState<Space | null>(null);
  const [files, setFiles] = useState<SpaceFile[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loadingSpaces, setLoadingSpaces] = useState(true);
  const [loadingData, setLoadingData] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);

  useEffect(() => {
    if (session?.user?.email) {
      loadSpaces();
    }
  }, [session?.user?.email]);

  const loadSpaces = async () => {
    setLoadingSpaces(true);
    try {
      const response = await fetch("/api/spaces");
      if (response.ok) {
        const data = await response.json();
        const list = Array.isArray(data) ? data : [];
        setSpaces(list);
        if (!selectedSpace && list.length) {
          selectSpace(list[0]);
        }
      }
    } catch (error) {
      console.error("Failed to load spaces:", error);
    } finally {
      setLoadingSpaces(false);
    }
  };

  const selectSpace = async (space: Space) => {
    setSelectedSpace(space);
    setLoadingData(true);
    await Promise.all([loadFiles(space.id), loadActivity(space.id)]);
    setLoadingData(false);
  };

  const refreshCurrentSpace = async () => {
    if (!selectedSpace) return;
    const response = await fetch(`/api/spaces/${selectedSpace.id}`);
    if (response.ok) {
      const updated = await response.json();
      setSelectedSpace(updated);
      setSpaces((prev) =>
        prev.map((space) => (space.id === updated.id ? updated : space))
      );
    }
  };

  const loadFiles = async (spaceId: string) => {
    try {
      const response = await fetch(`/api/spaces/${spaceId}/files`);
      if (response.ok) {
        const data = await response.json();
        setFiles(Array.isArray(data) ? data : []);
      } else {
        setFiles([]);
      }
    } catch (error) {
      console.error("Failed to load files:", error);
      setFiles([]);
    }
  };

  const loadActivity = async (spaceId: string) => {
    try {
      const response = await fetch(`/api/spaces/${spaceId}/activity`);
      if (response.ok) {
        const data = await response.json();
        setActivities(Array.isArray(data) ? data : []);
      } else {
        setActivities([]);
      }
    } catch (error) {
      console.error("Failed to load activity:", error);
      setActivities([]);
    }
  };

  const createSpace = async (name: string, description: string) => {
    try {
      const response = await fetch("/api/spaces", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, description }),
      });
      if (response.ok) {
        setShowCreateModal(false);
        await loadSpaces();
      } else {
        const error = await response.json().catch(() => ({}));
        alert(error.error || "Failed to create space");
      }
    } catch (error) {
      console.error("Failed to create space:", error);
    }
  };

  const uploadFile = async (file: File) => {
    if (!selectedSpace) return;
    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch(`/api/spaces/${selectedSpace.id}/files`, {
        method: "POST",
        body: formData,
      });
      if (response.ok) {
        setShowUploadModal(false);
        await loadFiles(selectedSpace.id);
        await loadActivity(selectedSpace.id);
      } else {
        const error = await response.json().catch(() => ({}));
        alert(error.error || "Failed to upload file");
      }
    } catch (error) {
      console.error("Failed to upload file:", error);
    }
  };

  const downloadFile = async (fileId: string, filename: string) => {
    try {
      const response = await fetch(`/api/files/${fileId}`);
      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      } else {
        alert("Failed to download file");
      }
    } catch (error) {
      console.error("Failed to download file:", error);
    }
  };

  const deleteFile = async (fileId: string) => {
    if (!selectedSpace) return;
    if (!confirm("Delete this file?")) return;

    try {
      const response = await fetch(`/api/files/${fileId}`, {
        method: "DELETE",
      });
      if (response.ok) {
        await loadFiles(selectedSpace.id);
        await loadActivity(selectedSpace.id);
      } else {
        const error = await response.json().catch(() => ({}));
        alert(error.error || "Failed to delete file");
      }
    } catch (error) {
      console.error("Failed to delete file:", error);
    }
  };

  if (loadingSpaces && !selectedSpace) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 flex">
      <aside className="w-72 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-5 border-b border-gray-100">
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-sm text-gray-500">Logged in as</p>
              <p className="text-sm font-semibold text-gray-800 truncate">
                {session?.user?.email}
              </p>
            </div>
            <button
              onClick={() => signOut()}
              className="text-xs text-red-600 hover:text-red-700"
            >
              Logout
            </button>
          </div>
          <button
            onClick={() => setShowCreateModal(true)}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white px-3 py-2 rounded-lg text-sm transition"
          >
            New Space
          </button>
        </div>

        <div className="flex-1 overflow-y-auto">
          {loadingSpaces && spaces.length === 0 ? (
            <div className="p-4 text-sm text-gray-500">Loading spaces…</div>
          ) : spaces.length === 0 ? (
            <div className="p-4 text-sm text-gray-500">
              No spaces yet. Create one to get started.
            </div>
          ) : (
            <ul className="py-2">
              {spaces.map((space) => (
                <li key={space.id}>
                  <button
                    className={`w-full text-left px-4 py-3 hover:bg-blue-50 transition ${
                      selectedSpace?.id === space.id
                        ? "bg-blue-100 border-l-4 border-blue-500"
                        : ""
                    }`}
                    onClick={() => selectSpace(space)}
                  >
                    <p className="font-semibold text-sm text-gray-800">
                      {space.name}
                    </p>
                    <p className="text-xs text-gray-500 truncate">
                      {space.description || "No description"}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </aside>

      <main className="flex-1 p-6 overflow-y-auto">
        {selectedSpace ? (
          <>
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
              <div>
                <h1 className="text-2xl font-bold text-gray-800">
                  {selectedSpace.name}
                </h1>
                <p className="text-sm text-gray-500 max-w-2xl">
                  {selectedSpace.description || "No description provided."}
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <button
                  onClick={() => setShowUploadModal(true)}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition"
                >
                  Upload File
                </button>
                <Link
                  href={`/spaces/${selectedSpace.id}/settings`}
                  className="bg-gray-900 hover:bg-black text-white px-4 py-2 rounded-lg transition"
                >
                  Open Settings
                </Link>
                <button
                  onClick={async () => {
                    await refreshCurrentSpace();
                    await Promise.all([
                      loadFiles(selectedSpace.id),
                      loadActivity(selectedSpace.id),
                    ]);
                  }}
                  className="border border-gray-300 px-4 py-2 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
                >
                  Refresh
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-6">
              <div className="bg-white rounded-lg shadow-sm border border-gray-100">
                <FilesPanel
                  files={files}
                  onDownload={downloadFile}
                  onDelete={deleteFile}
                />
              </div>
            </div>
          </>
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-center text-gray-500">
            <p className="text-lg">Select a space to get started.</p>
            <p className="text-sm">
              Use the sidebar to pick an existing space or create a new one.
            </p>
          </div>
        )}
      </main>

      {showCreateModal && (
        <CreateSpaceModal
          onClose={() => setShowCreateModal(false)}
          onCreate={createSpace}
        />
      )}

      {showUploadModal && (
        <UploadFileModal
          onClose={() => setShowUploadModal(false)}
          onUpload={uploadFile}
        />
      )}
    </div>
  );
}

function FilesPanel({
  files,
  onDownload,
  onDelete,
}: {
  files: SpaceFile[];
  onDownload: (id: string, name: string) => void;
  onDelete: (id: string) => void;
}) {
  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold text-gray-800">Files</h2>
      </div>
      <div className="space-y-3">
        {files.map((file) => (
          <div
            key={file.id}
            className="p-3 border border-gray-200 rounded-lg hover:bg-gray-50"
          >
            <div className="flex justify-between items-center">
              <div>
                <h4 className="font-medium text-gray-800">
                  {file.originalFilename}
                </h4>
                <p className="text-sm text-gray-600">
                  {formatFileSize(file.size)} • {file.uploaderEmail}
                </p>
                <p className="text-xs text-gray-500">
                  {new Date(file.uploadedAt).toLocaleDateString()}
                </p>
              </div>
              <div className="space-x-2">
                <button
                  onClick={() => onDownload(file.id, file.originalFilename)}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-sm"
                >
                  Download
                </button>
                <button
                  onClick={() => onDelete(file.id)}
                  className="bg-red-600 hover:bg-red-700 text-white px-3 py-1 rounded text-sm"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}
        {files.length === 0 && (
          <p className="text-sm text-gray-500">No files yet.</p>
        )}
      </div>
    </div>
  );
}

function ActivityPanel({
  activities,
  loading,
}: {
  activities: Activity[];
  loading: boolean;
}) {
  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-xl font-semibold text-gray-800 mb-4">
        Activity Log
      </h2>
      {loading ? (
        <p className="text-sm text-gray-500">Loading activity…</p>
      ) : activities.length === 0 ? (
        <p className="text-sm text-gray-500">No activity yet.</p>
      ) : (
        <div className="space-y-3 max-h-96 overflow-y-auto">
          {activities.map((activity) => (
            <div
              key={activity.id}
              className="border-l-4 border-blue-500 pl-4 py-2 bg-gray-50 rounded-r-lg"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="text-sm font-medium text-gray-800">
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
          ))}
        </div>
      )}
    </div>
  );
}

function CreateSpaceModal({
  onClose,
  onCreate,
}: {
  onClose: () => void;
  onCreate: (name: string, description: string) => void;
}) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
        <h3 className="text-lg font-semibold text-gray-800 mb-4">
          Create New Space
        </h3>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Space Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Enter space name"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Enter description (optional)"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 h-20"
            />
          </div>
        </div>
        <div className="flex justify-end space-x-3 mt-6">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-600 hover:text-gray-800"
          >
            Cancel
          </button>
          <button
            onClick={() => {
              if (name.trim()) {
                onCreate(name, description);
              }
            }}
            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg"
          >
            Create Space
          </button>
        </div>
      </div>
    </div>
  );
}

function UploadFileModal({
  onClose,
  onUpload,
}: {
  onClose: () => void;
  onUpload: (file: File) => void;
}) {
  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onUpload(file);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
        <h3 className="text-lg font-semibold text-gray-800 mb-4">Upload File</h3>
        <div className="space-y-4">
          <input
            type="file"
            onChange={handleFileChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="flex justify-end space-x-3 mt-6">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-600 hover:text-gray-800"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
}

function formatDateTime(timestamp: string): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return "just now";
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;

  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}


