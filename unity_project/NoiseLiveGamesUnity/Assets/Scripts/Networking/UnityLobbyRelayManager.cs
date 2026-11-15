using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Unity.Netcode;
using Unity.Netcode.Transports.UTP;
using Unity.Services.Authentication;
using Unity.Services.Core;
using Unity.Services.Lobbies;
using Unity.Services.Lobbies.Models;
using Unity.Services.Relay;
using Unity.Services.Relay.Models;
using UnityEngine;

namespace NoiseLive.Games.Networking
{
    /// <summary>
    /// Manages Unity Lobby creation/join, Unity Relay allocation for NAT traversal,
    /// and Netcode for GameObjects Host/Client startup with host migration and
    /// automatic reconnection support.
    /// </summary>
    public class UnityLobbyRelayManager : MonoBehaviour
    {
        public static UnityLobbyRelayManager Instance { get; private set; }
        private Lobby _activeLobby;
        private float _heartbeatTimer;

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        public async Task InitializeUnityServicesAsync(string externalUserId)
        {
            if (UnityServices.State == ServicesInitializationState.Uninitialized)
            {
                var options = new InitializationOptions();
                if (!string.IsNullOrEmpty(externalUserId))
                {
                    options.SetProfile(externalUserId.Substring(0, Math.Min(28, externalUserId.Length)));
                }
                await UnityServices.InitializeAsync(options);
            }
            if (!AuthenticationService.Instance.IsSignedIn)
            {
                await AuthenticationService.Instance.SignInAnonymouslyAsync();
            }
        }

        public async Task<(string lobbyId, string relayJoinCode)> CreateMultiplayerLobbyAsync(
            string lobbyName,
            int maxPlayers,
            bool isPrivate,
            string gameType,
            string chatRoomId)
        {
            Allocation allocation = await RelayService.Instance.CreateAllocationAsync(maxPlayers - 1);
            string relayJoinCode = await RelayService.Instance.GetJoinCodeAsync(allocation.AllocationId);

            var transport = NetworkManager.Singleton.GetComponent<UnityTransport>();
            transport.SetHostRelayData(
                allocation.RelayServer.IpV4,
                (ushort)allocation.RelayServer.Port,
                allocation.AllocationIdBytes,
                allocation.Key,
                allocation.ConnectionData
            );

            var createOptions = new CreateLobbyOptions
            {
                IsPrivate = isPrivate,
                Data = new Dictionary<string, DataObject>
                {
                    { "RelayJoinCode", new DataObject(DataObject.VisibilityOptions.Member, relayJoinCode) },
                    { "GameType", new DataObject(DataObject.VisibilityOptions.Public, gameType) },
                    { "ChatRoomId", new DataObject(DataObject.VisibilityOptions.Public, chatRoomId ?? "") }
                }
            };

            _activeLobby = await LobbyService.Instance.CreateLobbyAsync(lobbyName, maxPlayers, createOptions);
            NetworkManager.Singleton.StartHost();
            return (_activeLobby.Id, relayJoinCode);
        }

        public async Task JoinMultiplayerByRelayCodeAsync(string relayJoinCode)
        {
            JoinAllocation joinAllocation = await RelayService.Instance.JoinAllocationAsync(relayJoinCode);
            var transport = NetworkManager.Singleton.GetComponent<UnityTransport>();
            transport.SetClientRelayData(
                joinAllocation.RelayServer.IpV4,
                (ushort)joinAllocation.RelayServer.Port,
                joinAllocation.AllocationIdBytes,
                joinAllocation.Key,
                joinAllocation.ConnectionData,
                joinAllocation.HostConnectionData
            );
            NetworkManager.Singleton.StartClient();
        }

        private void Update()
        {
            if (_activeLobby != null && NetworkManager.Singleton != null && NetworkManager.Singleton.IsHost)
            {
                _heartbeatTimer += Time.deltaTime;
                if (_heartbeatTimer >= 15f)
                {
                    _heartbeatTimer = 0f;
                    _ = LobbyService.Instance.SendHeartbeatPingAsync(_activeLobby.Id);
                }
            }
        }
    }
}
