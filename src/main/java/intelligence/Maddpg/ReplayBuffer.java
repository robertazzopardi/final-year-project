package intelligence.Maddpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.nd4j.linalg.api.ndarray.INDArray;
import robots.Action;
import robots.RobotController;

/**
 * Collection of the previous experience in the simulation
 */
public class ReplayBuffer {
    private final List<Experience> buffer;

    public ReplayBuffer(final int maxSize) {
        this.buffer = new ArrayList<>(maxSize);
    }

    /**
     * Add new experiences to the memory
     *
     * @param state
     * @param action
     * @param reward
     * @param nextState
     * @param dones
     */
    public void push(final INDArray[] state, final Action[] action, final Float[] rewards,
            final INDArray[] nextState, final Integer[] dones) {
        buffer.add(new Experience(state, action, rewards, nextState, dones));
    }

    /**
     * Get a sample from the memory of size batchSize
     *
     * @param batchSize
     * @return Sample
     */
    public Sample sample(final int batchSize) {
        final List<List<INDArray>> obsBatch = new ArrayList<>(Arrays.asList(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<Action>> indivActionBatch = new ArrayList<>(Arrays.asList(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<Float>> indivRewardBatch = new ArrayList<>(Arrays.asList(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<INDArray>> nextObsBatch = new ArrayList<>(Arrays.asList(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

        final List<INDArray[]> globalStateBatch = new ArrayList<>();
        final List<INDArray[]> globalNextStateBatch = new ArrayList<>();
        final List<Action[]> globalActionsBatch = new ArrayList<>();
        final List<Integer> doneBatch = new ArrayList<>();

        final List<Experience> batch = randomSample(batchSize);

        for (final Experience experience : batch) {
            final INDArray[] state = experience.state;
            final Action[] action = experience.action;
            final Float[] reward = experience.reward;
            final INDArray[] nextState = experience.nextState;
            final Integer[] done = experience.dones;

            for (int i = 0; i < RobotController.AGENT_COUNT - 1; i++) {
                final INDArray obsI = state[i];
                final Action actionI = action[i];
                final Float rewardI = reward[i];
                final INDArray nextObsI = nextState[i];

                obsBatch.get(i).add(obsI);
                indivActionBatch.get(i).add(actionI);
                indivRewardBatch.get(i).add(rewardI);
                nextObsBatch.get(i).add(nextObsI);
            }

            globalStateBatch.add(Stream.of(state).flatMap(Stream::of).toArray(INDArray[]::new));
            globalActionsBatch.add(action);
            globalNextStateBatch
                    .add(Stream.of(nextState).flatMap(Stream::of).toArray(INDArray[]::new));
            doneBatch.addAll(Arrays.asList(done));
        }

        return new Sample(obsBatch, indivActionBatch, indivRewardBatch, nextObsBatch,
                globalStateBatch, globalNextStateBatch, globalActionsBatch, doneBatch);
    }

    public int getLength() {
        return buffer.size();
    }

    /**
     * Generate a random sample from a list
     *
     * Based on
     * https://stackoverflow.com/questions/8378752/pick-multiple-random-elements-from-a-list-in-java
     *
     * @param n
     * @return
     */
    public List<Experience> randomSample(final int n) {
        final List<Experience> copy = new ArrayList<>(buffer);
        Collections.shuffle(copy);
        return n > copy.size() ? copy.subList(0, copy.size()) : copy.subList(0, n);
    }

}
