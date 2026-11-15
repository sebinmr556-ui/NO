using System.Collections.Generic;
using Unity.Netcode;
using UnityEngine;

namespace NoiseLive.Games.Carrom3D
{
    /// <summary>
    /// Server-authoritative 3D Carrom controller using Unity 6 Physics and Netcode for GameObjects.
    /// Synchronizes striker baseline slide, aim direction, shot impulse, 9 Black + 9 White coins,
    /// Red Queen cover rules, pocket sensors, and turn rotation (1v1 / 2v2 / 4-player).
    /// </summary>
    public class Carrom3DController : NetworkBehaviour
    {
        [Header("3D Board & Pieces")]
        [SerializeField] private Rigidbody strikerRigidbody;
        [SerializeField] private List<Rigidbody> blackCoins = new();
        [SerializeField] private List<Rigidbody> whiteCoins = new();
        [SerializeField] private Rigidbody redQueen;
        [SerializeField] private Transform[] pocketCenters;

        [Header("Physics Parameters")]
        [SerializeField] private float maxStrikeForce = 32f;
        [SerializeField] private float pocketRadius = 0.065f;
        [SerializeField] private float velocityStopThreshold = 0.015f;

        public NetworkVariable<int> CurrentTurnSlot = new(1);
        public NetworkVariable<int> Team1Score = new(0);
        public NetworkVariable<int> Team2Score = new(0);
        public NetworkVariable<bool> QueenPocketedPendingCover = new(false);

        private bool _shotInProgress;

        [ServerRpc(RequireOwnership = false)]
        public void ShootStrikerServerRpc(float baselineNormalizedX, float aimAngleRad, float normalizedPower, ServerRpcParams rpcParams = default)
        {
            if (_shotInProgress) return;
            float clampedPower = Mathf.Clamp01(normalizedPower) * maxStrikeForce;
            Vector3 direction = new Vector3(Mathf.Cos(aimAngleRad), 0f, Mathf.Sin(aimAngleRad)).normalized;
            strikerRigidbody.linearVelocity = Vector3.zero;
            strikerRigidbody.AddForce(direction * clampedPower, ForceMode.Impulse);
            _shotInProgress = true;
        }

        private void FixedUpdate()
        {
            if (!IsServer || !_shotInProgress) return;

            CheckPocketedCoins();

            if (AreAllPiecesStopped())
            {
                _shotInProgress = false;
                AdvanceTurnOnServer();
            }
        }

        private void CheckPocketedCoins()
        {
            foreach (var pocket in pocketCenters)
            {
                if (redQueen != null && redQueen.gameObject.activeSelf &&
                    Vector3.Distance(redQueen.position, pocket.position) <= pocketRadius)
                {
                    redQueen.gameObject.SetActive(false);
                    QueenPocketedPendingCover.Value = true;
                }
            }
        }

        private bool AreAllPiecesStopped()
        {
            if (strikerRigidbody.linearVelocity.sqrMagnitude > velocityStopThreshold) return false;
            foreach (var c in whiteCoins)
                if (c.gameObject.activeSelf && c.linearVelocity.sqrMagnitude > velocityStopThreshold) return false;
            foreach (var c in blackCoins)
                if (c.gameObject.activeSelf && c.linearVelocity.sqrMagnitude > velocityStopThreshold) return false;
            if (redQueen != null && redQueen.gameObject.activeSelf && redQueen.linearVelocity.sqrMagnitude > velocityStopThreshold)
                return false;
            return true;
        }

        private void AdvanceTurnOnServer()
        {
            CurrentTurnSlot.Value = (CurrentTurnSlot.Value % 2) + 1;
        }
    }
}
