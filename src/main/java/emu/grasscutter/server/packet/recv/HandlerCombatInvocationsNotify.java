package emu.grasscutter.server.packet.recv;

import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.managers.MotionManager.MotionManager;
import emu.grasscutter.game.props.FightProperty;
import emu.grasscutter.game.props.LifeState;
import emu.grasscutter.net.packet.Opcodes;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.CombatInvocationsNotifyOuterClass.CombatInvocationsNotify;
import emu.grasscutter.net.proto.CombatInvokeEntryOuterClass.CombatInvokeEntry;
import emu.grasscutter.net.proto.EntityMoveInfoOuterClass.EntityMoveInfo;
import emu.grasscutter.net.proto.EvtBeingHitInfoOuterClass.EvtBeingHitInfo;
import emu.grasscutter.net.packet.PacketHandler;
import emu.grasscutter.net.proto.MotionStateOuterClass.MotionState;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.*;

@Opcodes(PacketOpcodes.CombatInvocationsNotify)
public class HandlerCombatInvocationsNotify extends PacketHandler {

	@Override
	public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
		CombatInvocationsNotify notif = CombatInvocationsNotify.parseFrom(payload);
		for (CombatInvokeEntry entry : notif.getInvokeListList()) {
			switch (entry.getArgumentType()) {
				case COMBAT_EVT_BEING_HIT:
					// Handle damage
					EvtBeingHitInfo hitInfo = EvtBeingHitInfo.parseFrom(entry.getCombatData());
					session.getPlayer().getAttackResults().add(hitInfo.getAttackResult());
					break;
				case ENTITY_MOVE:
					// Handle movement
					EntityMoveInfo moveInfo = EntityMoveInfo.parseFrom(entry.getCombatData());
					GameEntity entity = session.getPlayer().getScene().getEntityById(moveInfo.getEntityId());
					if (entity != null) {
						//move
						entity.getPosition().set(moveInfo.getMotionInfo().getPos());
						entity.getRotation().set(moveInfo.getMotionInfo().getRot());
						entity.setLastMoveSceneTimeMs(moveInfo.getSceneTime());
						entity.setLastMoveReliableSeq(moveInfo.getReliableSeq());
						entity.setMotionState(moveInfo.getMotionInfo().getState());
						session.getPlayer().getMotionManager().handle(session, entity, moveInfo);
					}
					break;
				default:
					break;
			}

			session.getPlayer().getCombatInvokeHandler().addEntry(entry.getForwardType(), entry);
		}

		if (notif.getInvokeListList().size() > 0) {
			session.getPlayer().getCombatInvokeHandler().update(session.getPlayer());
		}
                // Handle attack results last
		while (!session.getPlayer().getAttackResults().isEmpty()) {
			session.getPlayer().getScene().handleAttack(session.getPlayer().getAttackResults().poll());
		}
	}


}
