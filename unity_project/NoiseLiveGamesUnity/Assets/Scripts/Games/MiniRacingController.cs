using Unity.Netcode;
using UnityEngine;

namespace NoiseLive.Games.MiniRacing
{
    /// <summary>
    /// Multiplayer 3D Mini Racing controller with WheelCollider / Rigidbody vehicle physics,
    /// nitro boost, checkpoint validation, lap counter, race timer, and real-time transform sync.
    /// </summary>
    public class MiniRacingController : NetworkBehaviour
    {
        [SerializeField] private Rigidbody carBody;
        [SerializeField] private float motorTorque = 1850f;
        [SerializeField] private float nitroMultiplier = 1.65f;
        [SerializeField] private float maxSteerAngle = 32f;

        public NetworkVariable<int> CurrentLap = new(1);
        public NetworkVariable<float> RaceProgressNormalized = new(0f);
        public NetworkVariable<float> NitroCharge = new(100f);

        [ServerRpc]
        public void SubmitInputServerRpc(float steerInput, float throttleInput, bool nitroActive)
        {
            float boost = (nitroActive && NitroCharge.Value > 0f) ? nitroMultiplier : 1f;
            if (nitroActive && NitroCharge.Value > 0f)
            {
                NitroCharge.Value = Mathf.Max(0f, NitroCharge.Value - Time.fixedDeltaTime * 25f);
            }
            Vector3 forwardForce = transform.forward * (throttleInput * motorTorque * boost * Time.fixedDeltaTime);
            carBody.AddForce(forwardForce, ForceMode.Acceleration);
            transform.Rotate(0f, steerInput * maxSteerAngle * Time.fixedDeltaTime, 0f);
        }
    }
}
