package client.cn.kafei.simukraft.client.citizen;

import client.cn.kafei.simukraft.client.city.CityCoreScreenOpener;
import common.cn.kafei.simukraft.network.citizen.manage.CityCitizenManageResponsePacket;

public final class CityCitizenManageScreen {
    private CityCitizenManageScreen() {
    }

    public static void open(CityCitizenManageResponsePacket packet) {
        CityCoreScreenOpener.openCitizens(packet);
    }
}
