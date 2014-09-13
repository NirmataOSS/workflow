package com.nirmata.workflow.models;

import com.google.common.base.Preconditions;
import io.airlift.units.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Repetition
{
    private final Duration duration;
    private final Type type;

    public enum Type
    {
        RELATIVE,
        ABSOLUTE
    }

    public static final Repetition NONE = new Repetition();

    public Repetition(Duration duration, Type type)
    {
        this.duration = Preconditions.checkNotNull(duration, "duration cannot be null");
        this.type = Preconditions.checkNotNull(type, "type cannot be null");
    }

    public Date getNextDate(Date previousDate)
    {
        if ( duration.getValue() == 0 )
        {
            return null;
        }

        if ( type == Type.RELATIVE )
        {
            previousDate = new Date();
        }

        return new Date(duration.toMillis() + previousDate.getTime());
    }

    public Duration getDuration()
    {
        return duration;
    }

    public Type getType()
    {
        return type;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Repetition that = (Repetition)o;

        if ( !duration.equals(that.duration) )
        {
            return false;
        }
        //noinspection RedundantIfStatement
        if ( type != that.type )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = duration.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    private Repetition()
    {
        duration = new Duration(0, TimeUnit.MILLISECONDS);
        type = Type.ABSOLUTE;
    }
}
