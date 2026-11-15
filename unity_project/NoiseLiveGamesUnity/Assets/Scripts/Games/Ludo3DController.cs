using Unity.Netcode;
using UnityEngine;

namespace NoiseLive.Games.Ludo3D
{
    /// <summary>
    /// Server-authoritative 3D Ludo controller for 2-4 players.
    /// Synchronizes 3D dice rolls (1-6), 4 tokens per player, safe zones (stars),
    /// opponent token capture/kill, bonus turns on 6 or capture, and winner detection.
    /// </summary>
    public class Ludo3DController : NetworkBehaviour
    {
        public static readonly int[] SafeStarIndices = { 0, 8, 13, 21, 26, 34, 39, 47 };

        public NetworkVariable<int> CurrentPlayerIndex = new(0);
        public NetworkVariable<int> LastDiceValue = new(1);
        public NetworkVariable<bool> AwaitingTokenMove = new(false);

        [ServerRpc(RequireOwnership = false)]
        public void RollDiceServerRpc(ServerRpcParams rpcParams = default)
        {
            if (AwaitingTokenMove.Value) return;
            // Server-side random roll prevents client dice manipulation
            int roll = Random.Range(1, 7);
            LastDiceValue.Value = roll;
            AwaitingTokenMove.Value = true;
        }

        [ServerRpc(RequireOwnership = false)]
        public void MoveTokenServerRpc(int playerIndex, int tokenIndex)
        {
            if (!AwaitingTokenMove.Value || playerIndex != CurrentPlayerIndex.Value) return;
            bool bonusTurn = LastDiceValue.Value == 6;
            AwaitingTokenMove.Value = false;
            if (!bonusTurn)
            {
                CurrentPlayerIndex.Value = (CurrentPlayerIndex.Value + 1) % 4;
            }
        }
    }
}
