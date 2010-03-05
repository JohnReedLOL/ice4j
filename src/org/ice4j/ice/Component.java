/*
 * ice4j, the OpenSource Java Solution for NAT and Firewall Traversal.
 * Maintained by the SIP Communicator community (http://sip-communicator.org).
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.ice4j.ice;

import java.util.*;
import java.util.logging.*;

import org.ice4j.*;

/**
 * A component is a piece of a media stream requiring a single transport
 * address; a media stream may require multiple components, each of which has
 * to work for the media stream as a whole to work. For media streams based on
 * RTP, there are two components per media stream - one for RTP, and one for
 * RTCP.
 * <p>
 *
 * @author Emil Ivov
 * @author Namal Senarathne
 */
public class Component
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(Component.class.getName());

    /**
     * A component id is a positive integer between 1 and 256 which identifies
     * the specific component of the media stream for which this is a candidate.
     * It MUST start at 1 and MUST increment by 1 for each component of a
     * particular candidate. For media streams based on RTP, candidates for the
     * actual RTP media MUST have a component ID of 1, and candidates for RTCP
     * MUST have a component ID of 2. Other types of media streams which
     * require multiple components MUST develop specifications which define the
     * mapping of components to component IDs. See Section 14 for additional
     * discussion on extending ICE to new media streams.
     */
    private int componentID = -1;

    /**
     * The component ID to use with RTP streams.
     */
    public static final int RTP = 1;

    /**
     * The component ID to use with RTCP streams.
     */
    public static final int RTCP = 2;

    /**
     * The transport that this component is using.
     */
    private final Transport transport;

    /**
     * The <tt>IceMediaStream</tt> that this <tt>Component</tt> belongs to.
     */
    private final IceMediaStream parentStream;

    /**
     * The list locally gathered candidates for this media stream.
     */
    private List<LocalCandidate> localCandidates
                                        = new LinkedList<LocalCandidate>();

    /**
     * The list of candidates that the peer agent sent for this stream.
     */
    private List<Candidate> remoteCandidates = new LinkedList<Candidate>();

    /**
     * A <tt>Comparator</tt> that we use for sorting <tt>Candidate</tt>s by
     * their priority.
     */
    private CandidatePrioritizer candidatePrioritizer
                                              = new CandidatePrioritizer();

    /**
     * The default <tt>Candidate</tt> for this component or in other words, the
     * candidate that we would have used without ICE.
     */
    private LocalCandidate defaultCandidate = null;

    /**
     * The default <tt>RemoteCandidate</tt> for this component or in other
     * words, the candidate that we would have used to communicate with the
     * remote peer if we hadn't been using ICE.
     */
    private Candidate defaultRemoteCandidate = null;

    /**
     * Creates a new <tt>Component</tt> with the specified <tt>componentID</tt>
     * as a child of the specified <tt>MediaStream</tt>.
     *
     * @param componentID the id of this component.
     * @param transport the protocol that this component will be using (e.g.
     * TCP/UDP/TLS/DTLS).
     * @param mediaStream the {@link IceMediaStream} instance that would be the
     * parent of this component.
     */
    protected Component(int            componentID,
                        Transport      transport,
                        IceMediaStream mediaStream)
    {
        // the max value for componentID is 256
        this.componentID = componentID;
        this.transport = transport;
        this.parentStream = mediaStream;
    }

    /**
     * Add a local candidate to this component. The method should only be
     * accessed and local candidates added by the candidate harvesters
     * registered with the agent.
     *
     * @param candidate the candidate object to be added
     */
    public void addLocalCandidate(LocalCandidate candidate)
    {
        synchronized(localCandidates)
        {
            localCandidates.add(candidate);
        }
    }

    /**
     * Returns a copy of the list containing all local candidates currently
     * registered in this component.
     *
     * @return Returns a copy of the list containing all local candidates
     * currently registered in this <tt>Component</tt>.
     */
    public List<Candidate> getLocalCandidates()
    {
        synchronized(localCandidates)
        {
            return new ArrayList<Candidate>(localCandidates);
        }
    }

    /**
     * Returns the number of local host candidates currently registered in this
     * <tt>Component</tt>.
     *
     * @return the number of local host candidates currently registered in this
     * <tt>Component</tt>.
     */
    public int countLocalHostCandidates()
    {
        synchronized(localCandidates)
        {
            int count = 0;
            for(Candidate cand : localCandidates)
            {
                if (cand.getType() == CandidateType.HOST_CANDIDATE
                    && !cand.isVirtual())
                {
                    count++;
                }
            }

            return count;
        }
    }

    /**
     * Returns the number of all local candidates currently registered in this
     * <tt>Component</tt>.
     *
     * @return the number of all local candidates currently registered in this
     * <tt>Component</tt>.
     */
    public int getLocalCandidateCount()
    {
        synchronized(localCandidates)
        {
            return localCandidates.size();
        }
    }

    /**
     * Adds a remote <tt>Candidate</tt>s to this media-stream
     * <tt>Component</tt>.
     *
     * @param candidate the <tt>Candidate</tt> instance to add.
     */
    public void addRemoteCandidate(Candidate candidate)
    {
        synchronized(remoteCandidates)
        {
            remoteCandidates.add(candidate);
        }
    }

    /**
     * Returns a copy of the list containing all remote candidates currently
     * registered in this component.
     *
     * @return Returns a copy of the list containing all remote candidates
     * currently registered in this <tt>Component</tt>.
     */
    public List<Candidate> getRemoteCandidates()
    {
        synchronized(remoteCandidates)
        {
            return new ArrayList<Candidate>(remoteCandidates);
        }
    }

    /**
     * Adds a list of local <tt>Candidate</tt>s to this media-stream component.
     *
     * @param candidates a <tt>List</tt> of candidates to be added
     */
    public void addLocalCandidates(List<LocalCandidate> candidates)
    {
        synchronized(localCandidates)
        {
            localCandidates.addAll(candidates);
        }
    }

    /**
     * Adds a List of remote <tt>Candidate</tt>s as reported by a remote agent.
     *
     * @param candidates the <tt>List</tt> of <tt>Candidate</tt>s reported by
     * the remote agent for this component.
     */
    public void addRemoteCandidates(List<Candidate> candidates)
    {
        synchronized(remoteCandidates)
        {
            remoteCandidates.addAll(candidates);
        }
    }

    /**
     * Returns the number of all remote candidates currently registered in this
     * <tt>Component</tt>.
     *
     * @return the number of all remote candidates currently registered in this
     * <tt>Component</tt>.
     */
    public int getRemoteCandidateCount()
    {
        synchronized(remoteCandidates)
        {
            return remoteCandidates.size();
        }
    }

    /**
     * Returns a reference to the <tt>IceMediaStream</tt> that this
     * <tt>Component</tt> belongs to.
     *
     * @return  a reference to the <tt>IceMediaStream</tt> that this
     * <tt>Component</tt> belongs to.
     */
    public IceMediaStream getParentStream()
    {
        return parentStream;
    }

    /**
     * Returns the ID of this <tt>Component</tt>. For RTP/RTCP flows this would
     * be <tt>1</tt> for RTP and 2 for <tt>RTCP</tt>.
     *
     * @return the ID of this <tt>Component</tt>.
     */
    public int getComponentID()
    {
        return componentID;
    }

    /**
     * Returns the transport protocol of this component
     *
     * @return a {@link Transport} instance representing the the transport
     * protocol that this media stream <tt>Component</tt> uses.
     */
    public Transport getTransport()
    {
        return transport;
    }

    /**
     * Returns a <tt>String</tt> representation of this <tt>Component</tt>
     * containing its ID, parent stream name and any existing candidates.
     *
     * @return  a <tt>String</tt> representation of this <tt>Component</tt>
     * containing its ID, parent stream name and any existing candidates.
     */
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("Component id=").append(getComponentID());

        buff.append(" parent stream=" + getParentStream().getName());

        //local candidates
        int localCandidatesCount = getLocalCandidateCount();

        if(localCandidatesCount > 0)
        {
            buff.append("\nDefault Candidate: " + getDefaultCandidate());
            buff.append("\n" + localCandidatesCount + " local candidates:");

            synchronized(localCandidates)
            {
                for (Candidate cand : localCandidates)
                {
                    buff.append("\n" + cand.toString());
                }
            }
        }
        else
        {
            buff.append("\nno local candidates.");
        }

        //remote candidates
        int remoteCandidatesCount = getRemoteCandidateCount();

        if(remoteCandidatesCount > 0)
        {
            buff.append("\n" + remoteCandidatesCount + " remote candidates:");

            synchronized(remoteCandidates)
            {
                for (Candidate cand : remoteCandidates)
                {
                    buff.append("\n" + cand.toString());
                }
            }
        }
        else
        {
            buff.append("\nno remote candidates.");
        }

        return buff.toString();
    }

    /**
     * Computes the priorities of all <tt>Candidate</tt>s and then sorts them
     * accordingly.
     */
    protected void prioritizeCandidates()
    {
        synchronized(localCandidates)
        {
            LocalCandidate[] candidates
                = new LocalCandidate[localCandidates.size()];
            localCandidates.toArray(candidates);

            //first compute the actual priorities
            for (Candidate cand : candidates)
            {
                cand.computePriority();
            }

            //sort
            Arrays.sort(candidates, candidatePrioritizer);

            //now re-add the candidates in the order they've been sorted in.
            localCandidates.clear();
            for (LocalCandidate cand : candidates)
                localCandidates.add(cand);
        }
    }

    /**
     * Compares candidates based on their priority.
     */
    private static class CandidatePrioritizer
        implements Comparator<Candidate>
    {
        /**
         * Compares the two <tt>Candidate</tt>s based on their priority and
         * returns a negative integer, zero, or a positive integer as the first
         * <tt>Candidate</tt> has a lower, equal, or greater priority than the
         * second.
         *
         * @param c1 the first <tt>Candidate</tt> to compare.
         * @param c2 the second <tt>Candidate</tt> to compare.
         *
         * @return a negative integer, zero, or a positive integer as the first
         * <tt>Candidate</tt> has a lower, equal, or greater priority than the
         * second.
         */
        public int compare(Candidate c1, Candidate c2)
        {
            if(c1.getPriority() < c2.getPriority())
                return 1;
            else if(c1.getPriority() == c2.getPriority())
                return 0;
            else //if(c1.getPriority() > c2.getPriority())
                return -1;
        }

        /**
         * Indicates whether some other object is &quot;equal to&quot; this
         * Comparator.  This method must obey the general contract of
         * <tt>Object.equals(Object)</tt>.  Additionally, this method can return
         * <tt>true</tt> <i>only</i> if the specified Object is also a comparator
         * and it imposes the same ordering as this comparator.  Thus,
         * <code>comp1.equals(comp2)</code> implies that <tt>sgn(comp1.compare(o1,
         * o2))==sgn(comp2.compare(o1, o2))</tt> for every object reference
         * <tt>o1</tt> and <tt>o2</tt>.<p>
         *
         * Note that it is <i>always</i> safe <i>not</i> to override
         * <tt>Object.equals(Object)</tt>.  However, overriding this method may,
         * in some cases, improve performance by allowing programs to determine
         * that two distinct Comparators impose the same order.
         *
         * @param   obj   the reference object with which to compare.
         * @return  <code>true</code> only if the specified object is also
         *      a comparator and it imposes the same ordering as this
         *      comparator.
         * @see     java.lang.Object#equals(java.lang.Object)
         * @see java.lang.Object#hashCode()
         */
        public boolean equals(Object obj)
        {
            return (obj instanceof CandidatePrioritizer);
        }
    }

    /**
     * Eliminates redundant candidates, removing them from the specified
     * <tt>component</tt>.  A candidate is redundant if its transport address
     * equals another candidate, and its base equals the base of that other
     * candidate.  Note that two candidates can have the same transport address
     * yet have different bases, and these would not be considered redundant.
     * Frequently, a server reflexive candidate and a host candidate will be
     * redundant when the agent is not behind a NAT.  The agent SHOULD eliminate
     * the redundant candidate with the lower priority which is why we have to
     * run this method only after prioritizing candidates.
     */
    protected void eliminateRedundantCandidates()
    {
        List<Candidate> candidatesCopy = getLocalCandidates();

        for (Candidate cand : candidatesCopy)
        {
            //find and remove all candidates that have the same address and
            //base as cand and a lower priority.
            synchronized(localCandidates)
            {
                Iterator<LocalCandidate> localCandsIter
                                                = localCandidates.iterator();

                while (localCandsIter.hasNext())
                {
                    Candidate cand2 = localCandsIter.next();

                    if(   cand != cand2
                       && cand.getTransportAddress()
                                        .equals(cand2.getTransportAddress())
                       && cand.getBase().equals(cand2.getBase())
                       && cand.getPriority() >= cand2.getPriority() )
                    {
                       localCandsIter.remove();
                       if(logger.isLoggable(Level.FINEST))
                           logger.finest("eliminating redundant cand: "+ cand2);
                    }
                }
            }
        }
    }

    /**
     * Returns the <tt>Candidate</tt> that has been selected as the default
     * for this <tt>Component</tt> or <tt>null</tt> if no such
     * <tt>Candidate</tt> has been selected yet. A candidate is said to be
     * default if it would be the target of media from a non-ICE peer;
     *
     * @return the <tt>Candidate</tt> that has been selected as the default for
     * this <tt>Component</tt> or <tt>null</tt> if no such <tt>Candidate</tt>
     * has been selected yet
     */
    public LocalCandidate getDefaultCandidate()
    {
        return defaultCandidate;
    }

    /**
     * Returns the <tt>Candidate</tt> that the remote party has reported as
     * default for this <tt>Component</tt> or <tt>null</tt> if no such
     * <tt>Candidate</tt> has reported yet. A candidate is said to be
     * default if it would be the target of media from a non-ICE peer;
     *
     * @return the <tt>Candidate</tt> that the remote party has reported as
     * default for this <tt>Component</tt> or <tt>null</tt> if no such
     * <tt>Candidate</tt> has reported yet.
     */
    public Candidate getDefaultRemoteCandidate()
    {
        return defaultRemoteCandidate;
    }

    /**
     * Sets the <tt>Candidate</tt> that the remote party has reported as
     * default for this <tt>Component</tt>. A candidate is said to be
     * default if it would be the target of media from a non-ICE peer;
     *
     * @param candidate the <tt>Candidate</tt> that the remote party has
     * reported as default for this <tt>Component</tt>.
     */
    public void setDefaultRemoteCandidate(Candidate candidate)
    {
        this.defaultRemoteCandidate = candidate;
    }

    /**
     * Selects a <tt>Candidate</tt> that should be considered as the default
     * for this <tt>Component</tt>. A candidate is said to be default if it
     * would be the target of media from a non-ICE peer;
     * <p>
     * The ICE specification RECOMMENDEDs that default candidates be chosen
     * based on the likelihood of those candidates to work with the peer that is
     * being contacted. It is RECOMMENDED that the default candidates are the
     * relayed candidates (if relayed candidates are available), server
     * reflexive candidates (if server reflexive candidates are available), and
     * finally host candidates.
     * </p>
     */
    protected void selectDefaultCandidate()
    {
        synchronized(localCandidates)
        {
            Iterator<LocalCandidate> localCandsIter
                                                = localCandidates.iterator();

            while (localCandsIter.hasNext())
            {
                LocalCandidate cand = localCandsIter.next();

                if(this.defaultCandidate == null)
                {
                    this.defaultCandidate = cand;
                    continue;
                }

                if( defaultCandidate.getDefaultPreference()
                                < cand.getDefaultPreference())
                {
                    defaultCandidate = cand;
                }
            }
        }
    }

    /**
     * Releases all resources allocated by this <tt>Component</tt> and its
     * <tt>Candidate</tt>s like sockets for example.
     */
    protected void free()
    {
        synchronized (localCandidates)
        {
            Iterator<LocalCandidate> cndIter = localCandidates.iterator();
            while( cndIter.hasNext())
            {
                LocalCandidate candidate = cndIter.next();
                candidate.free();
                cndIter.remove();
            }
        }
    }
}
