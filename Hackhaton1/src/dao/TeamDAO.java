package dao;

import model.Team;
import java.util.List;

public interface TeamDAO {
    void creaTeam(Team team);
    List<Team> getTeamsDelHackathon(int hackathonId);
}