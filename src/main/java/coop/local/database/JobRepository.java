package coop.local.database;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
@Transactional(transactionManager = "piTransactionManager")
public class JobRepository {

    @Autowired
    @Qualifier("piSessionFactory")
    private  SessionFactory sessionFactory;

    public void updateStatus(Job job) {
        sessionFactory.getCurrentSession().createQuery("""
            update Job j
            set j.status = :status,
                j.rank = :rank
            where j.id = :id
            and j.rank < :rank

        """).setParameter("id", job.getId())
            .setParameter("rank", job.getStatus().rank())
            .setParameter("status", job.getStatus())
            .executeUpdate();

        sessionFactory.getCurrentSession().refresh(job);
    }

    public void persist(Job job) {
        sessionFactory.getCurrentSession().persist(job);
    }

    public Job findByFrameId(String frameId) {
        return sessionFactory
                .getCurrentSession()
                .createQuery("""
            from Job j
            where j.frameId = :frameId
        """, Job.class)
                .setParameter("frameId", frameId)
                .uniqueResultOptional()
                .orElse(null);
    }


    //TODO: Make the cutoff configuratble?
    public List<Job> findCreatedJobs() {
        long now = System.currentTimeMillis();

        return sessionFactory.getCurrentSession()
                .createQuery("""
            from Job j
            where j.status = :status
              and j.expireAt > :now
            order by j.createdAt asc
        """, Job.class)
                .setParameter("status", JobStatus.CREATED)
                .setParameter("now", now)
                .list();
    }

    //TODO: Make the cutoff configuratble?
    public List<Job> findPendingJobs() {
        long now = System.currentTimeMillis();

        return sessionFactory.getCurrentSession()
                .createQuery("""
            from Job j
            where j.status = :status
              and j.expireAt > :now
            order by j.createdAt asc
        """, Job.class)
                .setParameter("status", JobStatus.PENDING)
                .setParameter("now", now)
                .list();
    }

    // TODO: Make the cutoff configurable?
    public List<Job> findJobsUsingResources() {
        long now = System.currentTimeMillis();

        return sessionFactory.getCurrentSession()
                .createQuery("""
                from Job j
                where j.status in (
                    :pending,
                    :waitingForAck,
                    :waitingForComplete
                )
                  and j.expireAt > :now
                order by j.createdAt asc
            """, Job.class)
                .setParameter("pending", JobStatus.PENDING)
                .setParameter("waitingForAck", JobStatus.WAITING_FOR_ACK)
                .setParameter("waitingForComplete", JobStatus.WAITING_FOR_COMPLETE)
                .setParameter("now", now)
                .list();
    }

    public Job findWaitingForAck() {
        return sessionFactory.getCurrentSession()
                .createQuery("""
            from Job j
            where j.status = :status
            order by j.createdAt asc
        """, Job.class)
                .setParameter("status", JobStatus.WAITING_FOR_ACK)
                .setMaxResults(1)
                .uniqueResultOptional()
                .orElse(null);
    }

    public int purge() {
        long cutoffMillis = Instant.now().minusSeconds(48L * 60L * 60L).toEpochMilli();

        return sessionFactory.getCurrentSession()
                .createQuery("""
            delete from Job j
            where j.createdAt < :cutoff
        """)
                .setParameter("cutoff", cutoffMillis)
                .executeUpdate();
    }

    public void flush() {
        sessionFactory.getCurrentSession().flush();
    }

}
