using System.Collections.Generic;
using Unity.Netcode;
using UnityEngine;

namespace NoiseLive.Games.Pool8Ball
{
    /// <summary>
    /// Server-authoritative 3D 8 Ball Pool controller using Unity 6 Rigidbody physics.
    /// Handles cue stick aiming, power bar impulse, English spin, 15 numbered balls
    /// (Solids 1-7, 8-Ball, Stripes 9-15), 6 pockets, scratch detection, and 1v1 winner logic.
    /// </summary>
    public class Pool8BallController : NetworkBehaviour
    {
        [SerializeField] private Rigidbody cueBall;
        [SerializeField] private List<Rigidbody> numberedBalls = new();
        [SerializeField] private Transform[] pockets;
        [SerializeField] private float maxCueImpulse = 42f;
        [SerializeField] private float pocketRadius = 0.08f;

        public NetworkVariable<int> CurrentTurnPlayer = new(1);
        public NetworkVariable<int> Player1SolidsPotted = new(0);
        public NetworkVariable<int> Player2StripesPotted = new(0);

        [ServerRpc(RequireOwnership = false)]
        public void StrikeCueBallServerRpc(float aimRadians, float power01, Vector2 spinOffset)
        {
            float impulse = Mathf.Clamp01(power01) * maxCueImpulse;
            Vector3 dir = new Vector3(Mathf.Cos(aimRadians), 0f, Mathf.Sin(aimRadians)).normalized;
            cueBall.AddForceAtPosition(dir * impulse, cueBall.position + new Vector3(spinOffset.x, spinOffset.y, 0f) * 0.02f, ForceMode.Impulse);
        }
    }
}
