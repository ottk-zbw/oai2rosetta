package controllers;

import actors.CommandMessage;
import actors.Jobstatus;
import actors.RootActorSystem;
import actors.StatusMessage;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import models.Record;
import models.Repository;
import oai.*;
import play.Logger;
import play.data.Form;
import play.db.ebean.Model;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.Utils;
import views.html.monitor;
import views.html.records;

import java.util.*;

import static play.data.Form.form;
import static play.libs.Json.toJson;

/**
 * Created by Ott Konstantin on 21.08.2014.
 */

public class RepositoryApp extends Controller {
    static ActorSystem actorSystem = RootActorSystem.getInstance().getActorSystem();

    public static Result list() {
        return ok(views.html.repositories.render(""));
    }

    @Security.Authenticated(Secured.class)
    public static Result edit(int id) {
        Repository repository = Repository.findById(id);
        Form<Repository> repoForm  = form(Repository.class).fill(repository);
        return ok(views.html.repository.render(repoForm));
    }
    @Security.Authenticated(Secured.class)
    public static Result delete(int id) {
        Repository repository = Repository.findById(id);
        repository.delete();
        return redirect(routes.RepositoryApp.list());
    }

    @Security.Authenticated(Secured.class)
    public static Result submit() {
        Form<Repository> repoForm  = form(Repository.class).bindFromRequest();
        if (repoForm.hasErrors()) {
            flash("error","Bitte korrekt ausfüllen.");
            return badRequest(views.html.repository.render(repoForm));
        }
        Repository repos = repoForm.get();
        repos.update();
        flash("success", String.format("%s erfolgreich gespeichert.",repos.title));
        return list();
    }

    @Security.Authenticated(Secured.class)
    public static Result addRepository() {
        Repository repository = Form.form(Repository.class).bindFromRequest().get();
        repository.save();
        return redirect(routes.RepositoryApp.list());
    }

    public static Result getRepositories() {
        List<Repository> repositories = new Model.Finder(String.class, Repository.class).all();
        return ok(toJson(repositories));
    }

    public static Result getRecords(int repository_id) {
        CommandMessage msg = new CommandMessage(StatusMessage.GETRECORDJOB,false, repository_id,0);
        ActorSelection rootActor = actorSystem.actorSelection("user/RootGetRecordActor");
        rootActor.tell(msg,null);
        return ok();
    }



    public static Result showRepository(int id) {
        Repository repository = Repository.findById(id);
        int count = repository.records.size();
        return ok(records.render(repository));
    }

    @Security.Authenticated(Secured.class)
    public static Result monitor(int repository_id) {
        Repository repository = Repository.findById(repository_id);
        Map<String,Map<String,Integer>> stats = new HashMap<>();

        HashMap<String, Integer> reposStat = new HashMap<>();
        for (int i = 0; i < Record.ALLSTATUS.length; i++) {
            Integer count = repository.countStatus(Record.ALLSTATUS[i]);
            if (count>0)
            reposStat.put("" + Record.ALLSTATUS[i],count);
        }
        stats.put(repository.title,reposStat);

        //return ok(monitor.render(jobs,stats));
        return ok(monitor.render(stats));
    }

    @Security.Authenticated(Secured.class)
    public static Result monitorAll() {
        //Vector<Jobstatus> jobs = Utils.getJobMessages();
        // get Stats
        Map<String,Map<String,Integer>> stats = new HashMap<>();
        List<Repository> repositories = new Model.Finder(String.class, Repository.class).all();
        for (Repository repository: repositories) {
            HashMap<String, Integer> reposStat = new HashMap<>();
            for (int i = 0; i < Record.ALLSTATUS.length; i++) {
                Integer count = repository.countStatus(Record.ALLSTATUS[i]);
                if (count>0)
                    reposStat.put("" + Record.ALLSTATUS[i],count);
            }
            stats.put(repository.title,reposStat);
        }
        //return ok(monitor.render(jobs,stats));
        return ok(monitor.render(stats));
    }

    @Security.Authenticated(Secured.class)
    public static Result stop(String type) {
        Utils.stopJob(type);

        return ok();
    }

}
