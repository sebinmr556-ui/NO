using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

namespace NoiseLive.Games.Core
{
    [Serializable]
    public class NoisePlayerSlotDto
    {
        public int slotIndex;
        public string userId;
        public string username;
        public string displayName;
        public string avatarKey;
        public int team;
        public long score;
    }

    [Serializable]
    public class NoiseUnitySessionPayload
    {
        public string userId;
        public string username;
        public string displayName;
        public string profilePhoto;
        public string roomId;
        public string roomCode;
        public string chatRoomId;
        public string selectedGame;
        public string gameTitle;
        public string unitySceneName;
        public string unityLobbyId;
        public string unityRelayJoinCode;
        public string voiceChannelId;
        public bool liveKitVoiceConnected;
        public bool micMuted;
        public string graphicsQuality;
        public int targetFps;
        public float renderScale;
        public List<NoisePlayerSlotDto> playerList;
    }

    /// <summary>
    /// Attached to the persistent "NoiseUnityBridge" GameObject in the bootstrap scene.
    /// Receives session initialization JSON from the NOISE LIVE Android host app via
    /// UnityPlayer.UnitySendMessage("NoiseUnityBridge", "InitializeFromAndroidJson", payloadJson)
    /// or Intent extra "NOISE_UNITY_SESSION_PAYLOAD", configures URP quality & FPS,
    /// connects Unity Lobby/Relay, and loads the selected 3D game scene.
    /// </summary>
    public class NoiseUnityBridge : MonoBehaviour
    {
        public static NoiseUnityBridge Instance { get; private set; }
        public NoiseUnitySessionPayload CurrentSession { get; private set; }

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);
            ReadAndroidIntentPayloadIfPresent();
        }

        private void ReadAndroidIntentPayloadIfPresent()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                using var currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                using var intent = currentActivity.Call<AndroidJavaObject>("getIntent");
                string payloadJson = intent.Call<string>("getStringExtra", "NOISE_UNITY_SESSION_PAYLOAD");
                if (!string.IsNullOrEmpty(payloadJson))
                {
                    InitializeFromAndroidJson(payloadJson);
                }
            }
            catch (Exception e)
            {
                Debug.LogWarning($"[NoiseUnityBridge] Failed reading Android Intent payload: {e.Message}");
            }
#endif
        }

        public void InitializeFromAndroidJson(string payloadJson)
        {
            CurrentSession = JsonUtility.FromJson<NoiseUnitySessionPayload>(payloadJson);
            if (CurrentSession == null) return;

            ApplyGraphicsSettings(CurrentSession.graphicsQuality, CurrentSession.targetFps, CurrentSession.renderScale);

            if (!string.IsNullOrEmpty(CurrentSession.unitySceneName))
            {
                SceneManager.LoadSceneAsync(CurrentSession.unitySceneName, LoadSceneMode.Single);
            }
        }

        public void ApplyGraphicsSettings(string level, int targetFps, float renderScale)
        {
            Application.targetFrameRate = Mathf.Clamp(targetFps > 0 ? targetFps : 60, 30, 120);
            QualitySettings.vSyncCount = 0;

            if (GraphicsSettings.currentRenderPipeline is UniversalRenderPipelineAsset urpAsset)
            {
                urpAsset.renderScale = Mathf.Clamp(renderScale > 0f ? renderScale : 1.0f, 0.5f, 1.25f);
            }

            switch ((level ?? "HIGH").ToUpperInvariant())
            {
                case "LOW":
                    QualitySettings.SetQualityLevel(0, true);
                    break;
                case "MEDIUM":
                    QualitySettings.SetQualityLevel(1, true);
                    break;
                default:
                    QualitySettings.SetQualityLevel(2, true);
                    break;
            }
        }

        public void SendMatchResultToAndroid(string winnerUserId, string winnerUsername, long winnerScore, long runnerUpScore, long durationSec)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                using var currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                currentActivity.Call("onUnityMatchFinished", CurrentSession?.roomId ?? "", winnerUserId, winnerUsername, winnerScore, runnerUpScore, durationSec);
            }
            catch (Exception e)
            {
                Debug.LogWarning($"[NoiseUnityBridge] Failed sending match result to Android: {e.Message}");
            }
#endif
        }
    }
}
